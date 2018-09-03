package com.digitald4.common.util;

import com.digitald4.common.proto.DD4Protos.GeneralData;
import com.digitald4.common.proto.DD4UIProtos.ListResponse;
import com.google.protobuf.Any;
import org.junit.Test;

public class ProtoUtilTest {

	static {
		ProtoUtil.init(GeneralData.getDescriptor());
	}

	@Test
	public void testToJSON() {
		ProtoUtil.print(ListResponse.newBuilder()
				.addResult(Any.pack(GeneralData.newBuilder().setId(1).setName("Test 1").build()))
				.setTotalSize(10)
				.build());
	}
}
