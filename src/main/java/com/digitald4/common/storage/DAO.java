package com.digitald4.common.storage;

import com.digitald4.common.proto.DD4Protos.Query;
import com.google.protobuf.Message;
import java.util.function.UnaryOperator;

public interface DAO {

	<T extends Message> T create(T t);

	<T extends Message> T get(Class<T> c, long id);

	<T extends Message> QueryResult<T> list(Class<T> c, Query query);

	<T extends Message> T update(Class<T> c, long id, UnaryOperator<T> updater);

	<T> void delete(Class<T> c, long id);
}
