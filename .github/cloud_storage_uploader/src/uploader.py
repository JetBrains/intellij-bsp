from google.cloud.storage import Client, Bucket, Blob


class StorageUploader:
    _storage_client: Client

    def __init__(self, project_name: str):
        self._storage_client = Client(project=project_name)

    def upload_file_to_bucket(self, bucket_name: str, blob_name: str, file_path: str):
        with open(file_path, "rb") as f:
            bucket: Bucket = self._storage_client.bucket(bucket_name)
            blob: Blob = bucket.blob(blob_name)
            blob.upload_from_file(f)
