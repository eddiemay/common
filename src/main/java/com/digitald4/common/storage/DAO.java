package com.digitald4.common.storage;

import com.digitald4.common.proto.DD4Protos.Query;
import com.google.protobuf.GeneratedMessageV3;
import java.util.function.UnaryOperator;

public interface DAO {

	<T extends GeneratedMessageV3> T create(T t);

	<T extends GeneratedMessageV3> T get(Class<T> c, long id);

	<T extends GeneratedMessageV3> QueryResult<T> list(Class<T> c, Query query);

	<T extends GeneratedMessageV3> T update(Class<T> c, long id, UnaryOperator<T> updater);

	<T> void delete(Class<T> c, long id);
}
