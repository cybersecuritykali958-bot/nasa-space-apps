"""Скачивает и обрабатывает данные NASA, сохраняет результат в data/processed/.

Скрипт можно запускать повторно. Сырые файлы идут в data/raw/ (не коммитятся).
Заполни TODO под выбранный челлендж.
"""
import json
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
RAW = ROOT / "raw"
PROCESSED = ROOT / "processed"


def main() -> None:
    RAW.mkdir(exist_ok=True)
    PROCESSED.mkdir(exist_ok=True)

    # TODO: найти и скачать данные, например через earthaccess:
    #   import earthaccess
    #   earthaccess.login()  # EARTHDATA_USERNAME / EARTHDATA_PASSWORD из .env
    #   results = earthaccess.search_data(short_name="...", bounding_box=(...), temporal=(...))
    #   earthaccess.download(results, str(RAW))

    # TODO: обработать (pandas / xarray) и сохранить готовый JSON или GeoJSON.
    sample = {
        "type": "FeatureCollection",
        "features": [
            {
                "type": "Feature",
                "properties": {"name": "Пример", "value": 1},
                "geometry": {"type": "Point", "coordinates": [30.52, 50.45]},
            }
        ],
    }
    (PROCESSED / "sample.json").write_text(
        json.dumps(sample, ensure_ascii=False, indent=2), encoding="utf-8"
    )
    print("Готово: data/processed/sample.json")


if __name__ == "__main__":
    main()
