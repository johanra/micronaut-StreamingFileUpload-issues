package io.micronaut.upload;


import io.micronaut.runtime.Micronaut;

public class UploadApplication {

	public static void main(String[] args) {        
    	Micronaut.build(args)
    	.mainClass(UploadApplication.class)

    	.start();
    }
}
