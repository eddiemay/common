package com.digitald4.common.server.service;

import com.digitald4.common.exception.DD4StorageException;
import com.digitald4.common.model.User;
import com.digitald4.common.storage.SessionStore;
import com.digitald4.common.storage.Store;
import com.digitald4.common.storage.Query;
import com.digitald4.common.util.ProtoUtil;
import com.digitald4.common.storage.QueryResult;
import com.google.api.server.spi.ServiceException;
import com.google.api.server.spi.config.ApiMethod;
import com.google.api.server.spi.config.DefaultValue;
import com.google.api.server.spi.config.Named;
import com.google.api.server.spi.config.Nullable;
import com.google.protobuf.Descriptors;
import com.google.protobuf.Descriptors.Descriptor;
import com.google.protobuf.Descriptors.FieldDescriptor;
import com.google.protobuf.Message;
import java.util.Map;
import java.util.function.Function;

public class DualProtoService<T extends Message, I extends Message, U extends User>
		implements Createable<T>, Getable<T,Long>, Listable<T>, Updateable<T,Long>, Deleteable<T,Long> {

	private final T type;
	private final Store<I, Long> store;
	private final Descriptor internalDescriptor;
	private final Descriptor externalDescriptor;
	private final SessionStore<U> sessionStore;
	private final boolean requiresLoginDefault;

	private final Function<I, T> converter = new Function<I, T>() {
		@Override
		public T apply(I internal) {
			Message.Builder builder = type.newBuilderForType();
			for (Map.Entry<FieldDescriptor, Object> entry : internal.getAllFields().entrySet()) {
				FieldDescriptor field = externalDescriptor.findFieldByName(entry.getKey().getName());
				if (field != null) {
					try {
						if (field.getJavaType() == FieldDescriptor.JavaType.ENUM) {
							builder.setField(field, field.getEnumType()
									.findValueByNumber(((Descriptors.EnumValueDescriptor) entry.getValue()).getNumber()));
						} else {
							builder.setField(field, entry.getValue());
						}
					} catch (IllegalArgumentException iae) {
						throw new IllegalArgumentException("for field: " + field + " value: " + entry.getValue(), iae);
					}
				}
			}
			return (T) builder.build();
		}
	};
	
	private final Function<T, I> reverse = new Function<T, I>() {
		@Override
		public I apply(T external) {
			Message.Builder builder = store.getType().newBuilderForType();
			for (Map.Entry<FieldDescriptor, Object> entry : external.getAllFields().entrySet()) {
				FieldDescriptor field = internalDescriptor.findFieldByName(entry.getKey().getName());
				if (field != null) {
					if (field.getJavaType() == FieldDescriptor.JavaType.ENUM) {
						builder.setField(field, field.getEnumType()
								.findValueByNumber(((Descriptors.EnumValueDescriptor) entry.getValue()).getNumber()));
					} else {
						builder.setField(field, entry.getValue());
					}
				}
			}
			return (I) builder.build();
		}
	};
	
	public DualProtoService(Class<T> c, Store<I, Long> store, SessionStore<U> sessionStore, boolean requiresLoginDefault) {
		this(ProtoUtil.getDefaultInstance(c), store, sessionStore, requiresLoginDefault);
	}

	protected DualProtoService(T type, Store<I, Long> store, SessionStore<U> sessionStore, boolean requiresLoginDefault) {
		this.type = type;
		this.externalDescriptor = type.getDescriptorForType();
		this.store = store;
		this.internalDescriptor = store.getType().getDescriptorForType();
		this.sessionStore = sessionStore;
		this.requiresLoginDefault = requiresLoginDefault;
	}

	protected Store<T, Long> getStore() {
		return null;
	}

	public T getType() {
		return type;
	}

	protected Function<I, T> getConverter() {
		return converter;
	}

	protected Function<T, I> getReverseConverter() {
		return reverse;
	}

	@Override
	@ApiMethod(httpMethod = ApiMethod.HttpMethod.POST, path = "_")
	public T create(T entity, @Nullable @Named("idToken") String idToken) throws ServiceException {
		try {
			sessionStore.resolve(idToken, requiresLogin("create"));
			return getConverter().apply(store.create(getReverseConverter().apply(entity)));
		} catch (DD4StorageException e) {
			throw new ServiceException(e.getErrorCode(), e);
		}
	}

	@Override
	@ApiMethod(httpMethod = ApiMethod.HttpMethod.GET, path = "{id}")
	public T get(@Named("id") Long id, @Nullable @Named("idToken") String idToken) throws ServiceException {
		try {
			sessionStore.resolve(idToken, requiresLogin("create"));
			return getConverter().apply(store.get(id));
		} catch (DD4StorageException e) {
			throw new ServiceException(e.getErrorCode(), e);
		}
	}

	@Override
	@ApiMethod(httpMethod = ApiMethod.HttpMethod.GET, path = "_")
	public QueryResult<T> list(
			@Nullable @Named("filter") String filter, @Nullable @Named("orderBy") String orderBy,
			@Named("pageSize") @DefaultValue("0") int pageSize, @Named("pageToken") @DefaultValue("0") int pageToken,
			@Nullable @Named("idToken") String idToken) throws ServiceException {
		try {
			sessionStore.resolve(idToken, requiresLogin("create"));
			return toListResponse(store.list(Query.forList(filter, orderBy, pageSize, pageToken)));
		} catch (DD4StorageException e) {
			throw new ServiceException(e.getErrorCode(), e);
		}
	}

	@Override
	@ApiMethod(httpMethod = ApiMethod.HttpMethod.PUT, path = "{id}")
	public T update(@Named("id") Long id, T entity, @Named("updateMask") String updateMask,
									@Nullable @Named("idToken") String idToken) throws ServiceException {
		try {
			sessionStore.resolve(idToken, requiresLogin("create"));
			return getConverter().apply(
					store.update(id, internal -> ProtoUtil.merge(updateMask, getReverseConverter().apply(entity), internal)));
		} catch (DD4StorageException e) {
			throw new ServiceException(e.getErrorCode(), e);
		}
	}

	@Override
	@ApiMethod(httpMethod = ApiMethod.HttpMethod.DELETE, path = "id/{id}")
	public Empty delete(@Named("id") Long id, @Nullable @Named("idToken") String idToken) throws ServiceException {
		try {
			sessionStore.resolve(idToken, requiresLogin("create"));
			store.delete(id);
			return Empty.getInstance();
		} catch (DD4StorageException e) {
			throw new ServiceException(e.getErrorCode(), e);
		}
	}

	protected boolean requiresLogin(String method) {
		return requiresLoginDefault;
	}

	protected QueryResult<T> toListResponse(QueryResult<I> queryResult) {
		return QueryResult.transform(queryResult, getConverter());
	}
}
