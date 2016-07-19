package com.digitald4.common.server;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.digitald4.common.dao.QueryParam;
import com.digitald4.common.distributed.Function;
import com.digitald4.common.distributed.MultiCoreThreader;
import com.digitald4.common.exception.DD4StorageException;
import com.digitald4.common.proto.DD4UIProtos.DeleteRequest;
import com.digitald4.common.proto.DD4UIProtos.GetRequest;
import com.digitald4.common.proto.DD4UIProtos.UpdateRequest;
import com.digitald4.common.store.DAOStore;
import com.google.protobuf.Descriptors.FieldDescriptor;
import com.google.protobuf.GeneratedMessage;
import com.google.protobuf.Message;
import com.googlecode.protobuf.format.JsonFormat;
import com.googlecode.protobuf.format.JsonFormat.ParseException;

public class DualProtoService<T, I extends GeneratedMessage> implements ProtoService<T> {
	
	private final DAOStore<I> store;
	
	private final MultiCoreThreader threader = new MultiCoreThreader();
	
	private final Function<T, I> converter = new Function<T, I>() {
		@Override
		public T execute(I internal) {
			// TODO Auto-generated method stub
			return null;
		}
	};
	
	private final Function<I, T> reverse = new Function<I, T>() {
		@Override
		public I execute(T type) {
			// TODO Auto-generated method stub
			return null;
		}
	};
	
	public DualProtoService(DAOStore<I> store) {
		this.store = store;
	}
	
	public Function<T, I> getConverter() {
		return converter;
	}
	
	public Function<I, T> getReverseConverter() {
		return reverse;
	}

	@Override
	public T create(Message request) throws DD4StorageException {
		return getConverter().execute(store.create(getReverseConverter()
				.execute((T) request.getField(request.getDescriptorForType().findFieldByNumber(1)))));
	}

	@Override
	public T get(GetRequest request) throws DD4StorageException {
		return getConverter().execute(store.get(request.getId()));
	}

	@Override
	public List<T> list(Message request) throws DD4StorageException {
		List<QueryParam> params = new ArrayList<>();
		for (Map.Entry<FieldDescriptor, Object> entry : request.getAllFields().entrySet()) {
			params.add(new QueryParam(entry.getKey().getName(), "=", entry.getValue()));
		}
		return threader.parDo(store.get(params), getConverter());
	}

	@Override
	public T update(final UpdateRequest request) throws DD4StorageException {
		return getConverter().execute(store.update(request.getId(), new Function<I, I>() {
			@Override
			public I execute(I internal) {
				Message.Builder builder = internal.toBuilder();
				FieldDescriptor field = store.getDescriptor().findFieldByName(request.getProperty());
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
