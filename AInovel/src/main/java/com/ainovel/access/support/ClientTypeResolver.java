package com.ainovel.access.support;

import com.ainovel.access.contract.ClientType;
import java.util.Locale;
import java.util.Optional;
import org.springframework.stereotype.Component;

@Component
public class ClientTypeResolver {

    public Optional<ClientType> resolve(String value) {
        if (value == null || value.isBlank()) {
            return Optional.empty();
        }
        return switch (value.toLowerCase(Locale.ROOT)) {
            case "pc-web" -> Optional.of(ClientType.PC_WEB);
            case "h5" -> Optional.of(ClientType.H5);
            case "admin" -> Optional.of(ClientType.ADMIN);
            case "internal" -> Optional.of(ClientType.INTERNAL);
            default -> Optional.empty();
        };
    }
}
