package com.digitald4.common.server;

import com.digitald4.common.exception.DD4StorageException;
import com.digitald4.common.proto.DD4UIProtos.CreateRequest;
import com.digitald4.common.proto.DD4UIProtos.DeleteRequest;
import com.digitald4.common.proto.DD4UIProtos.GetRequest;
import com.digitald4.common.proto.DD4UIProtos.ListRequest;
import com.digitald4.common.proto.DD4UIProtos.UpdateRequest;
import com.digitald4.common.storage.DAOStore;
import com.google.protobuf.Descriptors.Descriptor;
import com.google.protobuf.GeneratedMessage;
import com.google.protobuf.Message;
import com.google.protobuf.Descriptors.FieldDescriptor;
import com.googlecode.protobuf.format.JsonFormat;
import com.googlecode.protobuf.format.JsonFormat.ParseException;

import java.util.List;
import java.util.function.UnaryOperator;

public class SingleProtoService<T extends GeneratedMessage> implements ProtoService<T> {
	
	private final DAOStore<T> store;
	private final Descriptor descriptor;
	
	public SingleProtoService(DAOStore<T> store) {
		this.store = store;
		this.descriptor = store.getType().getDescriptorForType();
	}

	@Override
	public T create(CreateRequest request) throws DD4StorageException {
		Message.Builder builder = store.getType().toBuilder();
		try {
			JsonFormat.merge(request.getProto(), builder);
		} catch (ParseException e) {
			throw new DD4StorageException("Error creating object", e);
		}
		return store.create((T) builder.build());
	}

	@Override
	public T get(GetRequest request) throws DD4StorageException {
		return store.get(request.getId());
	}

	@Override
	public List<T> list(ListRequest request) throws DD4StorageException {
		return store.get(request.getQueryParamList());
	}

	@Override
	public T update(final UpdateRequest request) throws DD4StorageException {
		return store.update(request.getId(), new UnaryOperator<T>() {
			@Override
			public T apply(T type) {
				Message.Builder builder = type.toBuilder();
				FieldDescriptor field = descriptor.findFieldByName(request.getProperty());
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
				return (T) builder.build();
			}
		});
	}

	@Override
	public boolean delete(DeleteRequest request) throws DD4StorageException {
		store.delete(request.getId());
		return true;
	}
}
