package io.micronaut.upload;


import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.StandardCopyOption;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;

import io.micronaut.core.annotation.NonNull;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.MediaType;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.PathVariable;
import io.micronaut.http.annotation.Post;
import io.micronaut.http.multipart.StreamingFileUpload;
import io.micronaut.objectstorage.ObjectStorageOperations;
import io.micronaut.objectstorage.request.FileUploadRequest;
import io.micronaut.objectstorage.request.UploadRequest;
import jakarta.validation.constraints.NotNull;

@Controller("/{tenant}/upload")
public class UploadController {
	
	final ObjectStorageOperations<?, ?, ?> objectStorageOperations;
	
	public UploadController(final ObjectStorageOperations<?, ?, ?> objectStorageOperations) {
		this.objectStorageOperations = objectStorageOperations;
	}
		
	@Post(uri = "/blobWithTemp/{id}", consumes = MediaType.MULTIPART_FORM_DATA)
    public HttpResponse<String> uploadBlobWithTemp(@NotNull final StreamingFileUpload file, @NotNull @PathVariable final String tenant, @NotNull @PathVariable final String id) {

		
		File tempfile;
		try {
			tempfile = File.createTempFile(tenant, id);
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
		try(InputStream inputStream = file.asInputStream()) {
			
			java.nio.file.Files.copy(
					inputStream, 
					tempfile.toPath(), 
				    StandardCopyOption.REPLACE_EXISTING);
			
			FileUploadRequest fileUploadRequest = new FileUploadRequest(id, null, tempfile.toPath(), Map.of());
			objectStorageOperations.upload(fileUploadRequest);
			return  HttpResponse.ok("Uploaded");
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
		finally {
			tempfile.delete();
		}
		
	}
	
	@Post(uri = "/blobDirect/{id}", consumes = MediaType.MULTIPART_FORM_DATA)
    public HttpResponse<String> uploadBlobDirect(@NotNull final StreamingFileUpload file, @NotNull @PathVariable final String tenant, @NotNull @PathVariable final String id) {

		StreamingFileUploadRequest uploadRequest = new StreamingFileUploadRequest(file, id);
		objectStorageOperations.upload(uploadRequest);
		
		return  HttpResponse.ok("Uploaded");
	}
	
	class StreamingFileUploadRequest implements UploadRequest {

	    @NonNull
	    private final StreamingFileUpload streamingFileUpload;
	    @NonNull
	    private final String key;

	    @NonNull
	    private Map<String, String> metadata;
	    
	
	    public StreamingFileUploadRequest(@NonNull StreamingFileUpload streamingFileUpload) {
	        this(streamingFileUpload, streamingFileUpload.getName(), Collections.emptyMap());
	    }

	    public StreamingFileUploadRequest(@NonNull StreamingFileUpload streamingFileUpload, @NonNull String key) {
	        this(streamingFileUpload, key, Collections.emptyMap());
	    }

	    public StreamingFileUploadRequest(@NonNull StreamingFileUpload streamingFileUpload, @NonNull String key, @NonNull Map<String, String> metadata) {
	        this.streamingFileUpload = streamingFileUpload;
	        this.key = key;
	        this.metadata = metadata;
	    }

	    @NonNull
	    @Override
	    public Optional<String> getContentType() {
	        return streamingFileUpload.getContentType()
	            .map(MediaType::getName);
	    }

	    @NonNull
	    @Override
	    public String getKey() {
	        return key;
	    }

	    @NonNull
	    @Override
	    public Optional<Long> getContentSize() {
	        return Optional.of(streamingFileUpload.getSize());
	    }

	    @NonNull
	    @Override
	    public InputStream getInputStream() {
	    	return streamingFileUpload.asInputStream();
	    }

	    @Override
	    @NonNull
	    public Map<String, String> getMetadata() {
	        return this.metadata;
	    }

	    @Override
	    public void setMetadata(@NonNull Map<String, String> metadata) {
	        this.metadata = metadata;
	    }
	}
}
