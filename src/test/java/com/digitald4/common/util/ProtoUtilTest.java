package com.digitald4.common.util;

import com.digitald4.common.proto.DD4Protos.GeneralData;
import com.digitald4.common.storage.Query;
import com.digitald4.common.storage.QueryResult;
import com.google.common.collect.ImmutableList;
import org.junit.Test;

public class ProtoUtilTest {

	@Test
	public void testToJSON() {
		System.out.println(
				ProtoUtil.toJSON(
						QueryResult.of(
								ImmutableList.of(
										GeneralData.newBuilder().setId(1).setName("Test 1").build(),
										GeneralData.newBuilder().setId(5).setName("Test 5").build()),
								10,
								null)));
	}
}
