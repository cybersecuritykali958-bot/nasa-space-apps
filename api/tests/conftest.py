import json
import os
import sys
from pathlib import Path

import pytest

API_DIR = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(API_DIR))


@pytest.fixture(scope="session")
def client(tmp_path_factory):
    tmp = tmp_path_factory.mktemp("api")
    data_dir = tmp / "processed"
    data_dir.mkdir()
    (data_dir / "sample.json").write_text(json.dumps({"ok": True}), encoding="utf-8")
    os.environ["DATABASE_URL"] = f"sqlite:///{tmp / 'test.db'}"
    os.environ["DATA_DIR"] = str(data_dir)

    from fastapi.testclient import TestClient

    from app.main import create_app

    with TestClient(create_app()) as c:
        yield c
