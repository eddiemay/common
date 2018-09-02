package com.digitald4.common.server;

import com.digitald4.common.exception.DD4StorageException;
import com.digitald4.common.proto.DD4Protos.Query;
import com.digitald4.common.proto.DD4Protos.Query.OrderBy;
import com.digitald4.common.proto.DD4UIProtos.BatchDeleteRequest;
import com.digitald4.common.proto.DD4UIProtos.BatchDeleteResponse;
import com.digitald4.common.proto.DD4UIProtos.CreateRequest;
import com.digitald4.common.proto.DD4UIProtos.DeleteRequest;
import com.digitald4.common.proto.DD4UIProtos.GetRequest;
import com.digitald4.common.proto.DD4UIProtos.ListRequest;
import com.digitald4.common.proto.DD4UIProtos.ListResponse;
import com.digitald4.common.proto.DD4UIProtos.UpdateRequest;
import com.digitald4.common.util.ProtoUtil;
import com.digitald4.common.storage.QueryResult;
import com.digitald4.common.storage.Store;
import com.google.api.server.spi.config.ApiMethod;
import com.google.protobuf.Any;
import com.google.protobuf.Descriptors;
import com.google.protobuf.Descriptors.Descriptor;
import com.google.protobuf.Descriptors.FieldDescriptor;
import com.google.protobuf.Empty;
import com.google.protobuf.GeneratedMessageV3;
import com.google.protobuf.Message;
import com.google.protobuf.util.FieldMaskUtil;
import com.google.protobuf.util.FieldMaskUtil.MergeOptions;
import java.util.Arrays;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.json.JSONObject;

public class DualProtoService<T extends GeneratedMessageV3, I extends GeneratedMessageV3>
		implements ProtoService<T>, JSONService {
	private static final MergeOptions MERGE_OPTIONS = new MergeOptions();
	static {
		MERGE_OPTIONS.setReplaceRepeatedFields(true);
	}

	private final Class<T> cls;
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
		this(ProtoUtil.getDefaultInstance(c), store);
	}

	protected DualProtoService(T type, Store<I> store) {
		this.cls = (Class<T>) type.getClass();
		this.type = type;
		this.externalDescriptor = type.getDescriptorForType();
		this.store = store;
		this.internalDescriptor = store.getType().getDescriptorForType();
	}

	@Override
	public T getType() {
		return type;
	}
	
	public Function<I, T> getConverter() {
		return converter;
	}
	
	public Function<T, I> getReverseConverter() {
		return reverse;
	}

	@Override
	@ApiMethod(httpMethod = ApiMethod.HttpMethod.POST)
	public T create(CreateRequest request) {
		return getConverter().apply(store.create(getReverseConverter().apply(ProtoUtil.unpack(cls, request.getEntity()))));
	}

	@Override
	@ApiMethod(httpMethod = ApiMethod.HttpMethod.GET)
	public T get(GetRequest request) {
		return getConverter().apply(store.get(request.getId()));
	}

	@Override
	public ListResponse list(ListRequest request) {
		return toListResponse(store.list(toQuery(request)));
	}

	@Override
	@ApiMethod(httpMethod = ApiMethod.HttpMethod.PUT)
	public T update(final UpdateRequest request) {
		return getConverter().apply(store.update(request.getId(), internal -> {
			Message.Builder builder = internal.toBuilder();
			T t = ProtoUtil.unpack(cls, request.getEntity());
			FieldMaskUtil.merge(request.getUpdateMask(), getReverseConverter().apply(t), builder, MERGE_OPTIONS);
			return (I) builder.build();
		}));
	}

	@Override
	@ApiMethod(httpMethod = ApiMethod.HttpMethod.DELETE)
	public Empty delete(DeleteRequest request) {
		store.delete(request.getId());
		return Empty.getDefaultInstance();
	}

	@Override
	public BatchDeleteResponse batchDelete(BatchDeleteRequest request) {
		return BatchDeleteResponse.newBuilder()
				.setDeleted(store.delete(toQuery(request)))
				.build();
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

	private Query toQuery(ListRequest request) {
		Query.Builder builder = Query.newBuilder()
				.setLimit(request.getPageSize())
				.setOffset(request.getPageToken())
				.addAllFilter(request.getFilterList().stream()
						.map(filter -> Query.Filter.newBuilder()
								.setColumn(filter.getColumn())
								.setOperator(filter.getOperator())
								.setValue(filter.getValue())
								.build())
						.collect(Collectors.toList()));
		if (!request.getOrderBy().isEmpty()) {
			builder.addAllOrderBy(Arrays.stream(request.getOrderBy().split(","))
					.map(orderBy -> OrderBy.newBuilder()
							.setColumn(orderBy.split(" ")[0])
							.setDesc(orderBy.endsWith("DESC"))
							.build())
					.collect(Collectors.toList()));
		}
		return builder.build();
	}

	private Query toQuery(BatchDeleteRequest request) {
		Query.Builder builder = Query.newBuilder()
				.setLimit(request.getPageSize())
				.setOffset(request.getPageToken())
				.addAllFilter(request.getFilterList().stream()
						.map(filter -> Query.Filter.newBuilder()
								.setColumn(filter.getColumn())
								.setOperator(filter.getOperator())
								.setValue(filter.getValue())
								.build())
						.collect(Collectors.toList()));
		if (!request.getOrderBy().isEmpty()) {
			builder.addAllOrderBy(Arrays.stream(request.getOrderBy().split(","))
					.map(orderBy -> OrderBy.newBuilder()
							.setColumn(orderBy.split(" ")[0])
							.setDesc(orderBy.endsWith("DESC"))
							.build())
					.collect(Collectors.toList()));
		}
		return builder.build();
	}

	@Override
	public JSONObject performAction(String action, JSONObject jsonRequest) {
		switch (action) {
			case "create":
				return toJSON(create(toProto(CreateRequest.getDefaultInstance(), jsonRequest)));
			case "get":
				return toJSON(get(toProto(GetRequest.getDefaultInstance(), jsonRequest)));
			case "list":
				return toJSON(list(toProto(ListRequest.getDefaultInstance(), jsonRequest)));
			case "update":
				return toJSON(update(toProto(UpdateRequest.getDefaultInstance(), jsonRequest)));
			case "delete":
				return toJSON(delete(toProto(DeleteRequest.getDefaultInstance(), jsonRequest)));
			case "batchDelete":
				return toJSON(batchDelete(toProto(BatchDeleteRequest.getDefaultInstance(), jsonRequest)));
			default:
				throw new DD4StorageException("Invalid action: " + action, HttpServletResponse.SC_BAD_REQUEST);
		}
	}

	public <R extends Message> R toProto(R msgRequest, HttpServletRequest request) {
		return toProto(msgRequest, new JSONObject(request.getParameterMap().values().iterator().next()[0]));
	}

	@SuppressWarnings("unchecked")
	public <R extends Message> R toProto(R msgRequest, JSONObject json) {
		R.Builder builder = msgRequest.toBuilder();
		if (json.has("entity")) {
			json.getJSONObject("entity")
					.put("@type", "type.googleapis.com/" + externalDescriptor.getFullName());
		}
		ProtoUtil.merge(json, builder);
		return (R) builder.build();
	}

	public static JSONObject toJSON(Message item) {
		return new JSONObject(ProtoUtil.print(item));
	}

	public static JSONObject toJSON(boolean bool) {
		return new JSONObject(bool);
	}
}
