package com.benito.sales.filter;

import com.benito.sales.helper.ErrorResponseBuilder;
import io.quarkus.security.identity.SecurityIdentity;
import io.vertx.ext.web.RoutingContext;
import jakarta.ws.rs.core.Response;
import org.jboss.resteasy.reactive.server.ServerRequestFilter;

import javax.net.ssl.SSLPeerUnverifiedException;

public class MtlsGuardFilter {


    private final SecurityIdentity _identity;

    public MtlsGuardFilter(SecurityIdentity identity)
    {
        this._identity = identity;
    }

    @ServerRequestFilter
    public Response requireCertificateForPrivilegedRoles(RoutingContext context)
    {

        boolean isPrivileged = _identity.hasRole("admin")
                               || _identity.hasRole("aService")
                               || _identity.hasRole("orderService");

        if (isPrivileged && !hasClientCertificate(context))
        {
            return ErrorResponseBuilder.build("FORBIDDEN",
                                              "A valid client certificate is required for this role",
                                              403);
        }

        return null;
    }

    private boolean hasClientCertificate(RoutingContext context)
    {
        try
        {
            return context.request().sslSession().getPeerCertificates().length > 0;
        }
        catch (SSLPeerUnverifiedException e)
        {
            return false;
        }
    }
}