package com.digitald4.common.util;

import javax.inject.Provider;

public class ProviderThreadLocalImpl<T> extends ThreadLocal<T> implements Provider<T> {}
