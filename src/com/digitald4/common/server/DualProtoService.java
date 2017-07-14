package com.digitald4.common.server;

import com.digitald4.common.exception.DD4StorageException;
import com.digitald4.common.proto.DD4UIProtos.CreateRequest;
import com.digitald4.common.proto.DD4UIProtos.DeleteRequest;
import com.digitald4.common.proto.DD4UIProtos.GetRequest;
import com.digitald4.common.proto.DD4UIProtos.ListRequest;
import com.digitald4.common.proto.DD4UIProtos.UpdateRequest;
import com.digitald4.common.storage.ListResponse;
import com.digitald4.common.storage.Store;
import com.google.protobuf.Descriptors;
import com.google.protobuf.Descriptors.Descriptor;
import com.google.protobuf.Descriptors.FieldDescriptor;
import com.google.protobuf.Empty;
import com.google.protobuf.GeneratedMessageV3;
import com.google.protobuf.Message;
import com.googlecode.protobuf.format.JsonFormat;
import com.googlecode.protobuf.format.JsonFormat.ParseException;
import java.lang.reflect.InvocationTargetException;
import java.util.Map;
import java.util.function.Function;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;
import javax.servlet.http.HttpServletRequest;
import org.json.JSONObject;

public class DualProtoService<T extends GeneratedMessageV3, I extends GeneratedMessageV3>
		implements ProtoService<T>, JSONService {
	
	private final T type;
	private final Store<I> store;
	private final Descriptor internalDescriptor;
	private final Descriptor externalDescriptor;
	
	private final Function<I, T> converter = new Function<I, T>() {
		@Override
		public T apply(I internal) {
			Message.Builder builder = type.newBuilderForType();
			for (Map.Entry<FieldDescriptor, Object> entry : internal.getAllFields().entrySet()) {
				FieldDescriptor field = externalDescriptor.findFieldByName(entry.getKey().getName());
				if (field != null) {
					try {
						switch (field.getJavaType()) {
							case ENUM:
								builder.setField(field, field.getEnumType()
										.findValueByNumber(((Descriptors.EnumValueDescriptor) entry.getValue()).getNumber()));
								break;
							default:
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
					switch (field.getJavaType()) {
						case ENUM:
							builder.setField(field, field.getEnumType()
									.findValueByNumber(((Descriptors.EnumValueDescriptor) entry.getValue()).getNumber()));
							break;
						default: builder.setField(field, entry.getValue());
					}
				}
			}
			return (I) builder.build();
		}
	};
	
	public DualProtoService(Class<T> c, Store<I> store) {
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

	protected DualProtoService(T type, Store<I> store) {
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
	public JSONObject create(JSONObject jsonRequest) {
		return convertToJSON(create(transformJSONRequest(CreateRequest.getDefaultInstance(), jsonRequest)));
	}

	@Override
	public T create(CreateRequest request) {
		Message.Builder builder = type.toBuilder();
		try {
			JsonFormat.merge(request.getProto(), builder);
		} catch (ParseException e) {
			throw new DD4StorageException("Error creating object: " + e.getMessage(), e);
		}
		return getConverter().apply(store.create(getReverseConverter().apply((T) builder.build())));
	}

	@Override
	public JSONObject get(JSONObject jsonRequest) {
		return convertToJSON(get(transformJSONRequest(GetRequest.getDefaultInstance(), jsonRequest)));
	}

	@Override
	public T get(GetRequest request) {
		return getConverter().apply(store.get(request.getId()));
	}

	@Override
	public JSONObject list(JSONObject jsonRequest) {
		return listToJSON.apply(list(transformJSONRequest(ListRequest.getDefaultInstance(), jsonRequest)));
	}

	@Override
	public ListResponse<T> list(ListRequest request) throws DD4StorageException {
		return toListResponse(store.list(request));
	}

	@Override
	public JSONObject update(JSONObject jsonRequest) {
		return convertToJSON(update(transformJSONRequest(UpdateRequest.getDefaultInstance(), jsonRequest)));
	}

	@Override
	public T update(final UpdateRequest request) throws DD4StorageException {
		return getConverter().apply(store.update(request.getId(), new UnaryOperator<I>() {
			@Override
			public I apply(I internal) {
				Message.Builder builder = internal.toBuilder();
				try {
					JsonFormat.merge(request.getProto(), builder);
				} catch (ParseException e) {
					throw new RuntimeException("Error updating object", e);
				}
				return (I) builder.build();
			}
		}));
	}

	@Override
	public JSONObject delete(JSONObject jsonRequest) {
		return convertToJSON(delete(transformJSONRequest(DeleteRequest.getDefaultInstance(), jsonRequest)));
	}

	@Override
	public Empty delete(DeleteRequest request) {
		store.delete(request.getId());
		return Empty.getDefaultInstance();
	}

	@Override
	public JSONObject performAction(String action, JSONObject jsonRequest) throws DD4StorageException {
		switch (action) {
			case "create":
				return create(jsonRequest);
			case "get":
				return get(jsonRequest);
			case "list":
				return list(jsonRequest);
			case "update":
				return update(jsonRequest);
			case "delete":
				return delete(jsonRequest);
			default:
				throw new DD4StorageException("Invalid action: " + action);
		}
	}

	public boolean requiresLogin(String action) {
		return true;
	}

	public ListResponse<T> toListResponse(ListResponse<I> response) {
		return ListResponse.<T>newBuilder()
				.addAllItems(response.getItemsList().stream()
						.map(getConverter())
						.collect(Collectors.toList()))
				.setTotalSize(response.getTotalSize())
				.build();
	}

	public static <R extends Message> R transformJSONRequest(R msgRequest, HttpServletRequest request) {
		return transformJSONRequest(msgRequest, new JSONObject(request.getParameterMap().values().iterator().next()[0]));
	}

	@SuppressWarnings("unchecked")
	public static <R extends Message> R transformJSONRequest(R msgRequest, JSONObject json) {
		R.Builder builder = msgRequest.toBuilder();
		try {
			JsonFormat.merge(json.toString(), builder);
		} catch (ParseException e) {
			throw new RuntimeException(e);
		}
		return (R) builder.build();
	}

	public static final Function<Message, JSONObject> messageToJSON = msg -> new JSONObject(JsonFormat.printToString(msg));

	public final Function<ListResponse<T>, JSONObject> listToJSON = response -> new JSONObject()
			.put("total_size", response.getTotalSize())
			.put("items", response.getItemsList().stream()
					.map(messageToJSON)
					.collect(Collectors.toList()));

	public static final JSONObject convertToJSON(Message item) {
		return new JSONObject(JsonFormat.printToString(item));
	}

	public static final JSONObject convertToJSON(boolean bool) {
		return new JSONObject(bool);
	}
}
