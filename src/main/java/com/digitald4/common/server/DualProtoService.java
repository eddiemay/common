package com.digitald4.common.server;

import com.digitald4.common.exception.DD4StorageException;
import com.digitald4.common.proto.DD4Protos.Query;
import com.digitald4.common.proto.DD4UIProtos.CreateRequest;
import com.digitald4.common.proto.DD4UIProtos.DeleteRequest;
import com.digitald4.common.proto.DD4UIProtos.GetRequest;
import com.digitald4.common.proto.DD4UIProtos.ListRequest;
import com.digitald4.common.proto.DD4UIProtos.ListResponse;
import com.digitald4.common.proto.DD4UIProtos.UpdateRequest;
import com.digitald4.common.storage.QueryResult;
import com.digitald4.common.storage.Store;
import com.google.protobuf.Any;
import com.google.protobuf.Descriptors;
import com.google.protobuf.Descriptors.Descriptor;
import com.google.protobuf.Descriptors.FieldDescriptor;
import com.google.protobuf.Empty;
import com.google.protobuf.GeneratedMessageV3;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.Message;
import com.google.protobuf.util.FieldMaskUtil;
import com.google.protobuf.util.FieldMaskUtil.MergeOptions;
import com.google.protobuf.util.JsonFormat;
import com.google.protobuf.util.JsonFormat.Parser;
import com.google.protobuf.util.JsonFormat.Printer;
import java.lang.reflect.InvocationTargetException;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import javax.servlet.http.HttpServletRequest;
import org.json.JSONObject;

public class DualProtoService<T extends GeneratedMessageV3, I extends GeneratedMessageV3>
		implements ProtoService<T>, JSONService {
	public static final MergeOptions MERGE_OPTIONS = new MergeOptions();
	static {
		MERGE_OPTIONS.setReplaceRepeatedFields(true);
	}

	private final T type;
	private final Store<I> store;
	private final Descriptor internalDescriptor;
	private final Descriptor externalDescriptor;
	private final Parser jsonParser;
	private final Printer jsonPrinter;

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
		} catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException
				| NoSuchMethodException | SecurityException e) {
			throw new RuntimeException(e);
		}
		this.externalDescriptor = type.getDescriptorForType();
		this.store = store;
		this.internalDescriptor = store.getType().getDescriptorForType();

		JsonFormat.TypeRegistry registry = JsonFormat.TypeRegistry.newBuilder().add(externalDescriptor).build();
		jsonParser = JsonFormat.parser().usingTypeRegistry(registry);
		jsonPrinter = JsonFormat.printer().usingTypeRegistry(registry);
	}

	protected DualProtoService(T type, Store<I> store) {
		this.type = type;
		this.externalDescriptor = type.getDescriptorForType();
		this.store = store;
		this.internalDescriptor = store.getType().getDescriptorForType();

		JsonFormat.TypeRegistry registry =
				JsonFormat.TypeRegistry.newBuilder().add(externalDescriptor).build();
		jsonParser = JsonFormat.parser().usingTypeRegistry(registry);
		jsonPrinter = JsonFormat.printer().usingTypeRegistry(registry);
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
		try {
			T t = request.getEntity().unpack((Class<T>) type.getClass());
			return getConverter().apply(store.create(getReverseConverter().apply(t)));
		} catch (InvalidProtocolBufferException e) {
			throw new RuntimeException(e);
		}
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
		return convertToJSON(list(transformJSONRequest(ListRequest.getDefaultInstance(), jsonRequest)));
	}

	@Override
	public ListResponse list(ListRequest request) {
		return toListResponse(store.list(toQuery(request)));
	}

	@Override
	public JSONObject update(JSONObject jsonRequest) {
		return convertToJSON(update(transformJSONRequest(UpdateRequest.getDefaultInstance(), jsonRequest)));
	}

	@Override
	public T update(final UpdateRequest request) {
		return getConverter().apply(store.update(request.getId(), internal -> {
			Message.Builder builder = internal.toBuilder();
			try {
				T t = request.getEntity().unpack((Class<T>) type.getClass());
				FieldMaskUtil.merge(request.getUpdateMask(), getReverseConverter().apply(t), builder, MERGE_OPTIONS);
			} catch (InvalidProtocolBufferException e) {
				throw new RuntimeException("Error updating object", e);
			}
			return (I) builder.build();
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
	public JSONObject performAction(String action, JSONObject jsonRequest) {
		switch (action) {
			case "create": return create(jsonRequest);
			case "get": return get(jsonRequest);
			case "list": return list(jsonRequest);
			case "update": return update(jsonRequest);
			case "delete": return delete(jsonRequest);
			default:
				throw new DD4StorageException("Invalid action: " + action);
		}
	}

	public boolean requiresLogin(String action) {
		return true;
	}

	public ListResponse toListResponse(QueryResult<I> queryResult) {
		return ListResponse.newBuilder()
				.addAllResult(queryResult.stream()
						.map(getConverter())
						.map(Any::pack)
						.collect(Collectors.toList()))
				.setTotalSize(queryResult.getTotalSize())
				.build();
	}

	public Query toQuery(ListRequest request) {
		return Query.newBuilder()
				.setLimit(request.getPageSize())
				.setOffset(request.getPageToken())
				.addAllFilter(request.getFilterList().stream()
						.map(filter -> Query.Filter.newBuilder()
								.setColumn(filter.getColumn())
								.setOperator(filter.getOperator())
								.setValue(filter.getValue())
								.build())
						.collect(Collectors.toList()))
				.addAllOrderBy(request.getOrderByList().stream()
						.map(orderyBy -> Query.OrderBy.newBuilder()
								.setColumn(orderyBy.getColumn())
								.setDesc(orderyBy.getDesc())
								.build())
						.collect(Collectors.toList()))
				.build();
	}

	public <R extends Message> R transformJSONRequest(R msgRequest, HttpServletRequest request) {
		return transformJSONRequest(msgRequest, new JSONObject(request.getParameterMap().values().iterator().next()[0]));
	}

	@SuppressWarnings("unchecked")
	public <R extends Message> R transformJSONRequest(R msgRequest, JSONObject json) {
		R.Builder builder = msgRequest.toBuilder();
		if (json.has("entity")) {
			json.getJSONObject("entity")
					.put("@type", "type.googleapis.com/" + externalDescriptor.getFullName());
		}
		jsonParser.merge(json.toString(), builder);
		return (R) builder.build();
	}

	public final JSONObject convertToJSON(Message item) {
		return new JSONObject(jsonPrinter.print(item));
	}

	public static final JSONObject convertToJSON(boolean bool) {
		return new JSONObject(bool);
	}
}
