package ch.hsr.ifs.pasta;

public class Pair<P, Q> {

	private final P first;
	private final Q second;

	public Pair(P first, Q second) {
		this.first = first;
		this.second = second;
	}
	public P getFirst() {
		return first;
	}
	public Q getSecond() {
		return second;
	}
}
