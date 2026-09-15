package com.cyys.application;

import com.cyys.common.satoken.RequestIdentity;
import com.cyys.common.satoken.ScopeContext;
import com.cyys.common.satoken.ScopeContextCleanupFilter;
import jakarta.servlet.ServletException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import java.util.ArrayList;
import java.util.List;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ScopeContextLifecycleTest {
    @AfterEach
    void clear() {
        ScopeContext.clear();
    }

    @Test
    void loginAndFailurePathsClearReusedThreadState() throws Exception {
        var filter = new ScopeContextCleanupFilter();
        ScopeContext.set(identity("stale"));
        var loginRequest = new MockHttpServletRequest("POST", "/api/auth/login");
        filter.doFilter(loginRequest, new MockHttpServletResponse(), (request, response) -> {
            assertThatThrownBy(ScopeContext::get).isInstanceOf(IllegalStateException.class);
            ScopeContext.set(identity("first"));
        });
        assertThatThrownBy(ScopeContext::get).isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> filter.doFilter(new MockHttpServletRequest("GET", "/api/auth/me"),
                new MockHttpServletResponse(), (request, response) -> {
                    ScopeContext.set(identity("failed"));
                    throw new ServletException("expected failure");
                })).isInstanceOf(ServletException.class);
        assertThatThrownBy(ScopeContext::get).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void identityCannotBeChangedOrMutatedAndIsNotInheritedByWorkers() throws Exception {
        var permissions = new ArrayList<>(List.of("read"));
        ScopeContext.set(new RequestIdentity("actor", "scope-a", List.of(), permissions, List.of()));
        permissions.add("write");
        assertThat(ScopeContext.get().permissions()).containsExactly("read");
        assertThatThrownBy(() -> ScopeContext.get().permissions().add("write")).isInstanceOf(UnsupportedOperationException.class);
        assertThatThrownBy(() -> ScopeContext.set(identity("other"))).isInstanceOf(IllegalStateException.class);
        try (var executor = java.util.concurrent.Executors.newSingleThreadExecutor()) {
            assertThat(executor.submit(() -> {
                try { ScopeContext.get(); return false; } catch (IllegalStateException expected) { return true; }
            }).get()).isTrue();
        }
    }

    private RequestIdentity identity(String actor) {
        return new RequestIdentity(actor, "scope-a", List.of(), List.of(), List.of());
    }
}
