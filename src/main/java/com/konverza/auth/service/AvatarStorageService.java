package com.konverza.auth.service;

import com.konverza.auth.exception.InvalidAvatarFileException;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.UUID;

/**
 * Stores uploaded avatar images on local disk under {app.upload-dir}/avatars,
 * served back via /uploads/** (see StaticResourceConfig) — see design.md's
 * "Avatar storage: local disk + static file serving" decision.
 */
@Service
public class AvatarStorageService {

    private static final Map<String, String> ALLOWED_CONTENT_TYPES = Map.of(
            "image/png", "png",
            "image/jpeg", "jpg",
            "image/webp", "webp"
    );

    @Value("${app.upload-dir}")
    private String uploadDir;

    public String store(UUID userId, MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new InvalidAvatarFileException("El archivo de imagen es obligatorio");
        }
        String extension = ALLOWED_CONTENT_TYPES.get(file.getContentType());
        if (extension == null) {
            throw new InvalidAvatarFileException("El archivo debe ser una imagen PNG, JPEG o WEBP");
        }
        try {
            Path avatarsDir = Paths.get(uploadDir, "avatars");
            Files.createDirectories(avatarsDir);
            deleteExistingAvatar(avatarsDir, userId);
            Path target = avatarsDir.resolve(userId + "." + extension);
            file.transferTo(target);
        } catch (IOException e) {
            throw new UncheckedIOException("No se pudo guardar el avatar", e);
        }
        return "/uploads/avatars/" + userId + "." + extension;
    }

    private void deleteExistingAvatar(Path avatarsDir, UUID userId) throws IOException {
        for (String extension : ALLOWED_CONTENT_TYPES.values()) {
            Files.deleteIfExists(avatarsDir.resolve(userId + "." + extension));
        }
    }
}
