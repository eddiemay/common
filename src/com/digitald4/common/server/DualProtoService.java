package com.digitald4.common.server;

import java.lang.reflect.InvocationTargetException;
import java.util.List;
import java.util.Map;

import com.digitald4.common.distributed.Function;
import com.digitald4.common.distributed.MultiCoreThreader;
import com.digitald4.common.exception.DD4StorageException;
import com.digitald4.common.proto.DD4UIProtos.CreateRequest;
import com.digitald4.common.proto.DD4UIProtos.DeleteRequest;
import com.digitald4.common.proto.DD4UIProtos.GetRequest;
import com.digitald4.common.proto.DD4UIProtos.ListRequest;
import com.digitald4.common.proto.DD4UIProtos.UpdateRequest;
import com.digitald4.common.storage.DAOStore;
import com.google.protobuf.Descriptors;
import com.google.protobuf.Descriptors.Descriptor;
import com.google.protobuf.Descriptors.FieldDescriptor;
import com.google.protobuf.GeneratedMessage;
import com.google.protobuf.Message;
import com.googlecode.protobuf.format.JsonFormat;
import com.googlecode.protobuf.format.JsonFormat.ParseException;

public class DualProtoService<T extends GeneratedMessage, I extends GeneratedMessage>
		implements ProtoService<T> {
	
	private final MultiCoreThreader threader = new MultiCoreThreader();
	
	private final T type;
	private final DAOStore<I> store;
	private final Descriptor internalDescriptor;
	private final Descriptor externalDescriptor;
	
	private final Function<T, I> converter = new Function<T, I>() {
		@Override
		public T execute(I internal) {
			Message.Builder builder = type.newBuilderForType();
			for (Map.Entry<FieldDescriptor, Object> entry : internal.getAllFields().entrySet()) {
				FieldDescriptor field = externalDescriptor.findFieldByName(entry.getKey().getName());
				if (field != null) {
					switch (field.getJavaType()) {
						case ENUM: builder.setField(field,
								field.getEnumType().findValueByNumber(((Descriptors.EnumValueDescriptor) entry.getValue()).getNumber()));
							break;
						default: builder.setField(field, entry.getValue());
					}
				}
			}
			return (T) builder.build();
		}
	};
	
	private final Function<I, T> reverse = new Function<I, T>() {
		@Override
		public I execute(T external) {
			Message.Builder builder = store.getType().newBuilderForType();
			for (Map.Entry<FieldDescriptor, Object> entry : external.getAllFields().entrySet()) {
				FieldDescriptor field = internalDescriptor.findFieldByName(entry.getKey().getName());
				if (field != null) {
					switch (field.getJavaType()) {
						case ENUM: builder.setField(field,
								field.getEnumType().findValueByNumber(((Descriptors.EnumValueDescriptor) entry.getValue()).getNumber()));
							break;
						default: builder.setField(field, entry.getValue());
					}
				}
			}
			return (I) builder.build();
		}
	};
	
	public DualProtoService(Class<T> c, DAOStore<I> store) {
		try {
			this.type = (T) c.getMethod("getDefaultInstance").invoke(null);
		} catch (IllegalAccessException | IllegalArgumentException
				| InvocationTargetException | NoSuchMethodException | SecurityException e) {
			throw new RuntimeException(e);
		}
		this.externalDescriptor = type.getDescriptorForType();
		this.store = store;
		this.internalDescriptor = store.getType().getDescriptorForType();
	}
	
	public Function<T, I> getConverter() {
		return converter;
	}
	
	public Function<I, T> getReverseConverter() {
		return reverse;
	}

	@Override
	public T create(CreateRequest request) throws DD4StorageException {
		Message.Builder builder = store.getType().toBuilder();
		try {
			JsonFormat.merge(request.getProto(), builder);
		} catch (ParseException e) {
			throw new DD4StorageException("Error creating object", e);
		}
		return getConverter().execute(store.create(getReverseConverter().execute((T) builder.build())));
	}

	@Override
	public T get(GetRequest request) throws DD4StorageException {
		return getConverter().execute(store.get(request.getId()));
	}

	@Override
	public List<T> list(ListRequest request) throws DD4StorageException {
		return threader.parDo(store.get(request.getQueryParamList()), getConverter());
	}

	@Override
	public T update(final UpdateRequest request) throws DD4StorageException {
		return getConverter().execute(store.update(request.getId(), new Function<I, I>() {
			@Override
			public I execute(I internal) {
				Message.Builder builder = internal.toBuilder();
				FieldDescriptor field = internalDescriptor.findFieldByName(request.getProperty());
				switch (field.getJavaType()) {
					case BOOLEAN: builder.setField(field, Boolean.valueOf(request.getValue())); break;
					case DOUBLE: builder.setField(field, Double.valueOf(request.getValue())); break;
					case ENUM: builder.setField(field,
							field.getEnumType().findValueByNumber(Integer.valueOf(request.getValue()))); break;
					case FLOAT: builder.setField(field, Float.valueOf(request.getValue())); break;
					case INT: builder.setField(field, Integer.valueOf(request.getValue())); break;
					case LONG: builder.setField(field, Long.valueOf(request.getValue())); break;
					case MESSAGE: try {
							builder.clearField(field);
							JsonFormat.merge(request.getValue(), builder);
						} catch (ParseException e) {
							e.printStackTrace();
						} break;
					case BYTE_STRING:
					case STRING: builder.setField(field, request.getValue()); break;
				}
				return (I) builder.build();
			}
		}));
	}

	@Override
	public boolean delete(DeleteRequest request) throws DD4StorageException {
		store.delete(request.getId());
		return true;
	}
}
