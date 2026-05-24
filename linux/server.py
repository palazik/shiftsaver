#!/usr/bin/env python3
"""
ShiftSaver Download Server
Wraps yt-dlp to provide no-watermark downloads from YouTube, TikTok, Instagram.
"""

import os
import json
import uuid
import subprocess
import threading
import logging
from pathlib import Path
from flask import Flask, request, jsonify, send_from_directory
from flask_cors import CORS

app = Flask(__name__)
CORS(app)

logging.basicConfig(level=logging.INFO, format="[%(levelname)s] %(message)s")
log = logging.getLogger("shiftsaver")

DOWNLOAD_DIR = Path(os.environ.get("DOWNLOAD_DIR", "/opt/shiftsaver/downloads"))
DOWNLOAD_DIR.mkdir(parents=True, exist_ok=True)

VERSION = "1.0.0"


def detect_platform(url: str) -> str:
    if "youtube.com" in url or "youtu.be" in url:
        return "youtube"
    elif "tiktok.com" in url:
        return "tiktok"
    elif "instagram.com" in url:
        return "instagram"
    return "unknown"


def build_ytdlp_args(url: str, output_path: str, quality: str = "best") -> list:
    """
    Build yt-dlp command arguments.
    - TikTok: use format without watermark (no_watermark or web)
    - YouTube/Instagram: best mp4
    """
    platform = detect_platform(url)
    base_args = [
        "yt-dlp",
        "--no-playlist",
        "--no-warnings",
        "--restrict-filenames",
        "-o", output_path,
        "--print-json",
        "--merge-output-format", "mp4",
    ]

    if platform == "tiktok":
        # Select format that avoids the TikTok watermark overlay
        base_args += [
            "-f", "download_addr-2/download_addr/play_addr/0",
            "--extractor-args", "tiktok:app_info=1233456789012345678",
        ]
    elif platform == "youtube":
        if quality == "best":
            base_args += ["-f", "bestvideo[ext=mp4]+bestaudio[ext=m4a]/best[ext=mp4]/best"]
        elif quality == "720p":
            base_args += ["-f", "bestvideo[height<=720][ext=mp4]+bestaudio[ext=m4a]/best[height<=720]"]
        elif quality == "480p":
            base_args += ["-f", "bestvideo[height<=480][ext=mp4]+bestaudio[ext=m4a]/best[height<=480]"]
        elif quality == "audio":
            base_args += ["-x", "--audio-format", "mp3", "--audio-quality", "0"]
    elif platform == "instagram":
        base_args += ["-f", "best[ext=mp4]/best"]

    base_args.append(url)
    return base_args


@app.route("/status", methods=["GET"])
def status():
    return jsonify({"status": "ok", "version": VERSION})


@app.route("/download", methods=["POST"])
def download():
    body = request.get_json(silent=True)
    if not body or "url" not in body:
        return jsonify({"success": False, "error": "Missing 'url' field"}), 400

    url = body["url"].strip()
    quality = body.get("quality", "best")

    if not url.startswith("http"):
        return jsonify({"success": False, "error": "Invalid URL"}), 400

    file_id = uuid.uuid4().hex[:12]
    output_template = str(DOWNLOAD_DIR / f"{file_id}_%(title)s.%(ext)s")
    args = build_ytdlp_args(url, output_template, quality)

    log.info(f"Downloading: {url}")

    try:
        result = subprocess.run(
            args,
            capture_output=True,
            text=True,
            timeout=300
        )

        if result.returncode != 0:
            err = result.stderr.strip().splitlines()[-1] if result.stderr.strip() else "yt-dlp error"
            log.error(f"yt-dlp failed: {err}")
            return jsonify({"success": False, "error": err})

        # Parse JSON output from yt-dlp (--print-json gives one JSON per video)
        meta = {}
        for line in result.stdout.splitlines():
            line = line.strip()
            if line.startswith("{"):
                try:
                    meta = json.loads(line)
                    break
                except json.JSONDecodeError:
                    pass

        # Find the actual output file
        filepath = None
        title = meta.get("title", "download")
        filename_safe = title.replace("/", "_").replace("\\", "_")[:60]

        # yt-dlp may rename the file; find what it wrote
        for f in sorted(DOWNLOAD_DIR.glob(f"{file_id}_*")):
            filepath = str(f)
            break

        if not filepath:
            return jsonify({"success": False, "error": "Output file not found after download"})

        filesize = Path(filepath).stat().st_size if filepath else None
        thumbnail = meta.get("thumbnail")

        log.info(f"Done: {filepath}")
        return jsonify({
            "success": True,
            "filename": Path(filepath).name,
            "filepath": filepath,
            "filesize": filesize,
            "title": meta.get("title", "download"),
            "thumbnail": thumbnail,
            "platform": detect_platform(url),
            "error": None
        })

    except subprocess.TimeoutExpired:
        return jsonify({"success": False, "error": "Download timed out (>5 min)"}), 500
    except Exception as e:
        log.exception("Unexpected error")
        return jsonify({"success": False, "error": str(e)}), 500


@app.route("/files/<path:filename>", methods=["GET"])
def serve_file(filename):
    """Optional: serve downloaded files directly."""
    return send_from_directory(DOWNLOAD_DIR, filename)


@app.route("/list", methods=["GET"])
def list_files():
    files = []
    for f in sorted(DOWNLOAD_DIR.iterdir()):
        if f.is_file():
            files.append({"name": f.name, "size": f.stat().st_size})
    return jsonify({"files": files})


if __name__ == "__main__":
    port = int(os.environ.get("PORT", 5050))
    host = os.environ.get("HOST", "0.0.0.0")
    log.info(f"ShiftSaver server starting on {host}:{port}")
    log.info(f"Downloads dir: {DOWNLOAD_DIR}")
    app.run(host=host, port=port, debug=False)
