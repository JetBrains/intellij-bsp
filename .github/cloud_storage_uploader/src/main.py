import os

from src.uploader import StorageUploader


def get_env_with_error(env_name: str) -> str:
    try:
        return os.environ[env_name]
    except Exception as e:
        raise RuntimeError(f"Provided env var ({env_name}) is not present in the environment", e)


def main():
    google_project_name = get_env_with_error("GOOGLE_PROJECT_NAME")
    plugin_bucket_name = get_env_with_error("PLUGIN_BUCKET_NAME")
    plugin_file_path = get_env_with_error("PLUGIN_FILE_PATH")
    plugin_blob_name = get_env_with_error("PLUGIN_BLOB_NAME")

    uploader = StorageUploader(google_project_name)
    uploader.upload_file_to_bucket(plugin_bucket_name, plugin_blob_name, plugin_file_path)

if __name__ == "__main__":
    main()