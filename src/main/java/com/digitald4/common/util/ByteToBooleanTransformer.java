package com.digitald4.common.util;

import com.google.api.server.spi.config.Transformer;

public class ByteToBooleanTransformer implements Transformer<Byte, Boolean> {
		@Override
		public Boolean transformTo(Byte aByte) {
			return aByte != 0;
		}

		@Override
		public Byte transformFrom(Boolean aBoolean) {
			return aBoolean ? (byte) 1 : (byte) 0;
		}
	}