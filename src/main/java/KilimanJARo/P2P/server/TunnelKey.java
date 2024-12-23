package KilimanJARo.P2P.server;

import java.util.Objects;

public class TunnelKey {
	private final String from;
	private final String thirdParty;
	private final String to;

	public TunnelKey(String person1, String person2, String person3) {
		this.from = person1;
		this.thirdParty = person2;
		this.to = person3;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}
		TunnelKey that = (TunnelKey) o;
		return Objects.equals(from, that.from) &&
				Objects.equals(thirdParty, that.thirdParty) &&
				Objects.equals(to, that.to);
	}

	@Override
	public int hashCode() {
		return Objects.hash(from, thirdParty, to);
	}
}