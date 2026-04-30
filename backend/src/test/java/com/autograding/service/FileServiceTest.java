package com.autograding.service;

import com.autograding.common.BusinessException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class FileServiceTest {

    @TempDir
    Path tempDir;

    private FileService fileService;

    @BeforeEach
    void setUp() {
        fileService = new FileService(tempDir.toString());
    }

    @Test
    void storeFile_shouldSucceed() throws IOException {
        MultipartFile file = new MockMultipartFile(
                "file", "test.c", "text/plain", "#include <stdio.h>".getBytes());

        String storedName = fileService.storeFile(file);

        assertNotNull(storedName);
        assertTrue(storedName.endsWith("_test.c") || storedName.contains("test.c"));
        assertTrue(Files.exists(tempDir.resolve(storedName)));
    }

    @Test
    void storeFile_shouldThrowWhenEmpty() {
        MultipartFile file = new MockMultipartFile("file", "empty.c", "text/plain", new byte[0]);

        assertThrows(BusinessException.class, () -> fileService.storeFile(file));
    }

    @Test
    void storeFile_shouldThrowWhenNoOriginalName() {
        MultipartFile file = new MockMultipartFile("file", null, "text/plain", "content".getBytes());

        assertThrows(BusinessException.class, () -> fileService.storeFile(file));
    }

    @Test
    void storeFile_shouldSanitizeFileName() throws IOException {
        MultipartFile file = new MockMultipartFile(
                "file", "test@file!.c", "text/plain", "content".getBytes());

        String storedName = fileService.storeFile(file);

        assertNotNull(storedName);
        assertFalse(storedName.contains("@"));
        assertFalse(storedName.contains("!"));
    }

    @Test
    void getFile_shouldReturnPath() throws IOException {
        MultipartFile file = new MockMultipartFile(
                "file", "readme.txt", "text/plain", "hello".getBytes());
        String storedName = fileService.storeFile(file);

        Path result = fileService.getFile(storedName);

        assertNotNull(result);
        assertTrue(Files.exists(result));
    }

    @Test
    void getFile_shouldThrowWhenNotFound() {
        assertThrows(BusinessException.class, () -> fileService.getFile("nonexistent.txt"));
    }

    @Test
    void getFile_shouldThrowWhenPathTraversal() {
        assertThrows(BusinessException.class, () -> fileService.getFile("../dangerous.txt"));
    }

    @Test
    void deleteFile_shouldSucceed() throws IOException {
        MultipartFile file = new MockMultipartFile(
                "file", "delete_me.txt", "text/plain", "bye".getBytes());
        String storedName = fileService.storeFile(file);

        boolean deleted = fileService.deleteFile(storedName);

        assertTrue(deleted);
        assertFalse(Files.exists(tempDir.resolve(storedName)));
    }

    @Test
    void deleteFile_shouldReturnFalseForNonExistent() {
        boolean deleted = fileService.deleteFile("nonexistent.txt");
        assertFalse(deleted);
    }

    @Test
    void deleteFile_shouldReturnFalseForPathTraversal() {
        boolean deleted = fileService.deleteFile("../outside.txt");
        assertFalse(deleted);
    }
}
