package org.apromore.service.loganimation.recording;

import org.eclipse.collections.api.set.primitive.IntSet;
import org.eclipse.collections.api.set.primitive.MutableIntSet;
import org.eclipse.collections.impl.factory.primitive.IntSets;

public class TokenCluster {
    private MutableIntSet tokens;
    private double distance;
    
    private TokenCluster(IntSet tokens, double distance) {
        this.tokens = IntSets.mutable.ofAll(tokens);
        this.distance = distance;
    }
    
    public static TokenCluster of(IntSet tokens, double distance) {
        return new TokenCluster(tokens, distance);
    }
    
    public boolean addToken(int token) {
        return tokens.add(token);
    }
    
    public IntSet getTokens() {
        return tokens.toImmutable();
    }
    
    public void removeToken(int token) {
        tokens.remove(token);
    }
    
    public boolean isEmpty() {
        return tokens.isEmpty();
    }
    
    public double getDistance() {
        return distance;
    }
    
    public void increaseDistance(double increase) {
        if (increase <= 0) return;
        this.distance += increase;
    }
    
    public int size() {
        return tokens.size();
    }
    
    public int getOneToken() {
        return !isEmpty() ? tokens.intIterator().next() : -1;
    }
    
    public TokenCluster copy() {
        return new TokenCluster(tokens, distance);
    }
}
