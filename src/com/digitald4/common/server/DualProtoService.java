package com.digitald4.common.server;

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
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.lang.reflect.InvocationTargetException;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;

public class DualProtoService<T extends GeneratedMessage, I extends GeneratedMessage>
		implements ProtoService<T>, JSONService {
	
	private final T type;
	private final DAOStore<I> store;
	private final Descriptor internalDescriptor;
	private final Descriptor externalDescriptor;
	
	private final Function<I, T> converter = new Function<I, T>() {
		@Override
		public T apply(I internal) {
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
	
	private final Function<T, I> reverse = new Function<T, I>() {
		@Override
		public I apply(T external) {
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

	protected DualProtoService(T type, DAOStore<I> store) {
		this.type = type;
		this.externalDescriptor = type.getDescriptorForType();
		this.store = store;
		this.internalDescriptor = store.getType().getDescriptorForType();
	}
	
	public Function<I, T> getConverter() {
		return converter;
	}
	
	public Function<T, I> getReverseConverter() {
		return reverse;
	}

	@Override
	public T create(CreateRequest request) throws DD4StorageException {
		Message.Builder builder = store.getType().toBuilder();
		try {
			JsonFormat.merge(request.getProto(), builder);
		} catch (ParseException e) {
			throw new DD4StorageException("Error creating object: " + e.getMessage(), e);
		}
		return getConverter().apply(store.create(getReverseConverter().apply((T) builder.build())));
	}

	@Override
	public T get(GetRequest request) throws DD4StorageException {
		return getConverter().apply(store.get(request.getId()));
	}

	@Override
	public List<T> list(ListRequest request) throws DD4StorageException {
		return store.get(request.getQueryParamList()).stream().map(getConverter()).collect(Collectors.toList());
	}

	@Override
	public T update(final UpdateRequest request) throws DD4StorageException {
		return getConverter().apply(store.update(request.getId(), new UnaryOperator<I>() {
			@Override
			public I apply(I internal) {
				Message.Builder builder = internal.toBuilder();
				for (UpdateRequest.Update update : request.getUpdateList()) {
					FieldDescriptor field = internalDescriptor.findFieldByName(update.getProperty());
					String value = update.getValue();
					switch (field.getJavaType()) {
						case BOOLEAN:
							builder.setField(field, Boolean.valueOf(value));
							break;
						case DOUBLE:
							builder.setField(field, Double.valueOf(value));
							break;
						case ENUM:
							builder.setField(field, field.getEnumType().findValueByNumber(Integer.valueOf(value)));
							break;
						case FLOAT:
							builder.setField(field, Float.valueOf(value));
							break;
						case INT:
							builder.setField(field, Integer.valueOf(value));
							break;
						case LONG:
							builder.setField(field, Long.valueOf(value));
							break;
						case MESSAGE:
							try {
								builder.clearField(field);
								JsonFormat.merge(value, builder);
							} catch (ParseException e) {
								e.printStackTrace();
							}
							break;
						case BYTE_STRING:
						case STRING:
							builder.setField(field, value);
							break;
					}
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

	public JSONObject performAction(String action, String jsonRequest)
			throws DD4StorageException, JSONException, ParseException {
		JSONObject json = new JSONObject();
		switch (action) {
			case "get":
				json.put("data", JSONService.convertToJSON(
						get(JSONService.transformJSONRequest(GetRequest.getDefaultInstance(), jsonRequest))));
				break;
			case "list":
				json.put("data", JSONService.convertToJSON(
						list(JSONService.transformJSONRequest(ListRequest.getDefaultInstance(), jsonRequest))));
				break;
			case "create":
				json.put("data", JSONService.convertToJSON(
						create(JSONService.transformJSONRequest(CreateRequest.getDefaultInstance(), jsonRequest))));
				break;
			case "update":
				json.put("data", JSONService.convertToJSON(
						update(JSONService.transformJSONRequest(UpdateRequest.getDefaultInstance(), jsonRequest))));
				break;
			case "delete":
				json.put("data", JSONService.convertToJSON(
						delete(JSONService.transformJSONRequest(DeleteRequest.getDefaultInstance(), jsonRequest))));
				break;
			default: throw new DD4StorageException("Invalid action: " + action);
		}
		return json;
	}
}
