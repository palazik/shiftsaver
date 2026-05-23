from __future__ import annotations

import asyncio
import ipaddress
import json
import os
import re
import socket
import uuid
from dataclasses import asdict, dataclass
from pathlib import Path
from typing import Literal
from urllib.parse import urlparse

from fastapi import FastAPI, HTTPException
from fastapi.responses import FileResponse
from pydantic import BaseModel, HttpUrl

APP_VERSION = "0.1.0"
DOWNLOAD_DIR = Path(os.environ.get("SHIFTSAVER_DOWNLOAD_DIR", str(Path.home() / "ShiftSaverDownloads"))).resolve()
MAX_CONCURRENT_DOWNLOADS = int(os.environ.get("SHIFTSAVER_MAX_JOBS", "2"))
ALLOWED_HOSTS = (
    "youtube.com",
    "youtu.be",
    "tiktok.com",
    "vm.tiktok.com",
    "instagram.com",
    "www.youtube.com",
    "www.tiktok.com",
    "www.instagram.com",
)

app = FastAPI(title="ShiftSaver Server", version=APP_VERSION)
jobs: dict[str, "Job"] = {}
queue = asyncio.Semaphore(MAX_CONCURRENT_DOWNLOADS)


class DownloadRequest(BaseModel):
    url: HttpUrl


@dataclass
class Job:
    id: str
    url: str
    status: Literal["queued", "running", "done", "error"]
    title: str | None = None
    file_name: str | None = None
    error: str | None = None


@app.get("/health")
async def health() -> dict[str, object]:
    return {"ok": True, "version": APP_VERSION, "download_dir": str(DOWNLOAD_DIR), "ip": local_ip()}


@app.post("/downloads")
async def create_download(request: DownloadRequest) -> dict[str, object]:
    url = str(request.url)
    validate_url(url)
    job = Job(id=uuid.uuid4().hex, url=url, status="queued")
    jobs[job.id] = job
    asyncio.create_task(run_download(job))
    return asdict(job)


@app.get("/downloads/{job_id}")
async def get_download(job_id: str) -> dict[str, object]:
    job = jobs.get(job_id)
    if job is None:
        raise HTTPException(status_code=404, detail="Unknown download job")
    return asdict(job)


@app.get("/files/{file_name}")
async def get_file(file_name: str) -> FileResponse:
    target = (DOWNLOAD_DIR / file_name).resolve()
    if not str(target).startswith(str(DOWNLOAD_DIR)) or not target.is_file():
        raise HTTPException(status_code=404, detail="File not found")
    return FileResponse(target, filename=target.name)


async def run_download(job: Job) -> None:
    async with queue:
        job.status = "running"
        DOWNLOAD_DIR.mkdir(parents=True, exist_ok=True)
        output_template = str(DOWNLOAD_DIR / "%(title).180B [%(id)s].%(ext)s")
        command = [
            "yt-dlp",
            "--no-playlist",
            "--restrict-filenames",
            "--windows-filenames",
            "--merge-output-format",
            "mp4",
            "--print",
            "after_move:filepath",
            "--output",
            output_template,
            job.url,
        ]
        process = await asyncio.create_subprocess_exec(
            *command,
            stdout=asyncio.subprocess.PIPE,
            stderr=asyncio.subprocess.PIPE,
        )
        stdout, stderr = await process.communicate()
        if process.returncode != 0:
            job.status = "error"
            job.error = clean_error(stderr.decode("utf-8", errors="replace"))
            return
        paths = [line.strip() for line in stdout.decode("utf-8", errors="replace").splitlines() if line.strip()]
        file_path = Path(paths[-1]).resolve() if paths else newest_file()
        if file_path is None or not file_path.is_file():
            job.status = "error"
            job.error = "Download finished but the output file was not found"
            return
        job.file_name = file_path.name
        job.title = title_from_file(file_path.name)
        job.status = "done"


def validate_url(url: str) -> None:
    parsed = urlparse(url)
    host = (parsed.hostname or "").lower()
    if parsed.scheme not in {"http", "https"}:
        raise HTTPException(status_code=400, detail="Only http and https URLs are supported")
    if not any(host == allowed or host.endswith("." + allowed) for allowed in ALLOWED_HOSTS):
        raise HTTPException(status_code=400, detail="Only public YouTube, TikTok, and Instagram URLs are enabled")
    try:
        ipaddress.ip_address(host)
        raise HTTPException(status_code=400, detail="Direct IP URLs are not accepted")
    except ValueError:
        pass


def clean_error(raw: str) -> str:
    compact = re.sub(r"\s+", " ", raw).strip()
    return compact[-900:] if compact else "yt-dlp failed"


def newest_file() -> Path | None:
    files = [path for path in DOWNLOAD_DIR.iterdir() if path.is_file()]
    return max(files, key=lambda path: path.stat().st_mtime, default=None)


def title_from_file(name: str) -> str:
    stem = Path(name).stem
    return re.sub(r"\s*\[[^\]]+]$", "", stem).replace("_", " ").strip() or name


def local_ip() -> str:
    sock = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
    try:
        sock.connect(("8.8.8.8", 80))
        return sock.getsockname()[0]
    except OSError:
        return "127.0.0.1"
    finally:
        sock.close()


if __name__ == "__main__":
    print(json.dumps({"version": APP_VERSION, "ip": local_ip()}))
