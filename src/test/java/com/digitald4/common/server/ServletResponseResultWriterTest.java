package com.digitald4.common.server;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.digitald4.common.proto.DD4Protos.User;
import com.digitald4.common.util.ByteToBooleanTransformer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.google.api.server.spi.ConfiguredObjectMapper;
import com.google.api.server.spi.ObjectMapperUtil;
import com.google.api.server.spi.config.model.ApiSerializationConfig;
import com.google.api.server.spi.response.ServletResponseResultWriter;
import com.google.common.collect.ImmutableList;
import com.hubspot.jackson.datatype.protobuf.ProtobufModule;
import java.io.PrintWriter;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;

public class ServletResponseResultWriterTest {
	private ServletResponseResultWriter servletResponseResultWriter;

	// ObjectWriter objectWriter = new ObjectMapper().disable(SerializationFeature.FAIL_ON_EMPTY_BEANS).writer();

	private ObjectWriter objectWriter = ConfiguredObjectMapper.builder()
			.addRegisteredModules(ImmutableList.of(new ProtobufModule()))
			.build()
			.writer();

	@Mock private HttpServletResponse response = Mockito.mock(HttpServletResponse.class);
	@Mock private ServletOutputStream printWriter = Mockito.mock(ServletOutputStream.class);

	@Before
	public void setup() throws Exception {
		ApiSerializationConfig config = new ApiSerializationConfig();
		config.addSerializationConfig(ByteToBooleanTransformer.class);
		when(response.getOutputStream()).thenReturn(printWriter);
		servletResponseResultWriter = new ServletResponseResultWriter(response, config);
	}

	@Test
	public void testWriteLong() throws Exception {
		servletResponseResultWriter.write(52L);
		// verify(printWriter).print("\"52\"");
	}

	@Test
	public void testWriteString() throws Exception {
		servletResponseResultWriter.write("Hello World");
		// verify(printWriter).print("\"Hello World\"");
	}

	@Test
	public void testWritePojo() throws Exception {
		servletResponseResultWriter.write(new Pojo().setId(52L).setName("Eddie"));
		// verify(printWriter).print("{\"id\":\"52\",\"name\":\"Eddie\"}");
	}

	@Test
	public void testWriteStruct() throws Exception {
		servletResponseResultWriter.write(new Struct(52L, "Eddie"));
		// verify(printWriter).print("{\"id\":\"52\",\"name\":\"Eddie\"}");
	}

	@Test
	public void testWriteByteToBoolean() throws Exception {
		servletResponseResultWriter.write((byte) 82);
		// verify(printWriter).print("true");
	}

	@Test
	public void testWriteProto() throws Exception {
		servletResponseResultWriter.write(User.newBuilder().setId(52L).setUsername("eddiemay").build());
		// verify(printWriter).print("{\"id\":\"52\",\"username\":\"eddiemay\"}");
	}

	@Test
	public void objectWriterWriteProto() throws Exception {
		objectWriter.writeValue(printWriter, User.newBuilder().setId(52L).setUsername("eddiemay").build());
		// verify(printWriter).write("{\"id\":52,\"typeId\":0,\"username\":\"eddiemay\",\"email\":\"\",\"firstName\":\"\",\"lastName\":\"\",\"fullName\":\"\",\"disabled\":false,\"readOnly\":false,\"notes\":\"\",\"lastLogin\":0,\"idToken\":\"\",\"expTime\":0,\"password\":\"\"}");
	}

	public class Pojo {
		private long id;
		private String name;

		public long getId() {
			return id;
		}

		public Pojo setId(long id) {
			this.id = id;
			return this;
		}

		public String getName() {
			return name;
		}

		public Pojo setName(String name) {
			this.name = name;
			return this;
		}
	}

	public class Struct {
		public final long id;
		public final String name;

		public Struct(long id, String name) {
			this.id = id;
			this.name = name;
		}

		public Builder toBuilder() {
			return new Builder(this);
		}

		public class Builder {
			public long id;
			public String name;

			public Builder() {};

			public Builder(Struct struct){
				this.id = struct.id;
				this.name = struct.name;
			}

			public Struct build() {
				return new Struct(id, name);
			}
		}
	}
}
