package com.backend.proxy.context;

/**
 * ThreadLocal para propagar el contexto de la sesión de Flowable durante la petición actual.
 */
public final class FlowableSessionHolder {

    private static final ThreadLocal<FlowableSessionContext> CONTEXT = new ThreadLocal<>();

    private FlowableSessionHolder() {}

    public static void set(FlowableSessionContext context) {
        CONTEXT.set(context);
    }

    public static FlowableSessionContext get() {
        return CONTEXT.get();
    }

    public static String getFlowableJSessionId() {
        FlowableSessionContext ctx = get();
        return ctx != null ? ctx.flowableJSessionId() : null;
    }

    public static void clear() {
        CONTEXT.remove();
    }
}
