package pl.com.sofitfy.enhanced_containers.testutils;

import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.mockbukkit.mockbukkit.MockBukkit;

/**
 * Problem:
 * - W nowszym Paper API część typów (np. ItemStack) może wymagać zainicjalizowanego Bukkit.server.
 *
 * Rozwiązanie praktyczne:
 * - Startujemy "goły" serwer MockBukkit raz na cały run testów.
 * - Nie ładujemy pluginu ani nie symulujemy eventów.
 *
 * Uwaga:
 * - To kompromis. Docelowo testy jednostkowe powinny unikać Paper API.
 */
public final class BukkitUnitTestBootstrapExtension implements BeforeAllCallback {

    private static final ExtensionContext.Namespace NAMESPACE = ExtensionContext.Namespace.create(
        BukkitUnitTestBootstrapExtension.class
    );

    @Override
    public void beforeAll(ExtensionContext context) {
        // Testy integracyjne same kontrolują MockBukkit.
        if (context.getTags().contains("integration")) {
            return;
        }

        // Startujemy raz na cały run (root store jest współdzielony).
        context.getRoot().getStore(NAMESPACE).getOrComputeIfAbsent(
            "mockbukkit",
            key -> new MockBukkitResource(),
            MockBukkitResource.class
        );
    }

    private static final class MockBukkitResource implements ExtensionContext.Store.CloseableResource {
        private MockBukkitResource() {
            if (!MockBukkit.isMocked()) {
                MockBukkit.mock();
            }
        }

        @Override
        public void close() {
            if (MockBukkit.isMocked()) {
                MockBukkit.unmock();
            }
        }
    }
}
