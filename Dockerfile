FROM gradle:8.4-jdk17 AS plugin-build-image

WORKDIR /
COPY . .
RUN gradle build --no-daemon


FROM python:3.12 AS python-build-image

RUN pip install poetry

ENV POETRY_NO_INTERACTION=1 \
    POETRY_VIRTUALENVS_IN_PROJECT=1 \
    POETRY_VIRTUALENVS_CREATE=1 \
    POETRY_CACHE_DIR=/tmp/poetry_cache

WORKDIR /app

COPY .github/cloud_storage_uploader/pyproject.toml .github/cloud_storage_uploader/poetry.lock ./
RUN --mount=type=cache,target=$POETRY_CACHE_DIR poetry install --no-root


FROM python:3.12 AS runtime-image

WORKDIR /app
ENV VIRTUAL_ENV=/app/.venv \
    PATH="$/app/.venv/bin:$PATH" \
    PYTHONPATH="/app/.venv/lib/python3.12/site-packages"

ENV GOOGLE_APPLICATION_CREDENTIALS="/app/google_credentials.json" \
    GOOGLE_PROJECT_NAME="mapz-zpp" \
    PLUGIN_BUCKET_NAME="mapz-intellij-bsp" \
    PLUGIN_FILE_PATH="/intellij-bsp.zip" \
    PLUGIN_BLOB_NAME="intellij-bsp-mapz.zip"

COPY --from=python-build-image /app/.venv /app/.venv
COPY --from=plugin-build-image /build/distributions/intellij-bsp-2024.1.0-EAP.zip /intellij-bsp.zip
COPY .github/cloud_storage_uploader/src src
COPY google_credentials.json .

ENTRYPOINT [ "python", "-m", "src.main" ]
