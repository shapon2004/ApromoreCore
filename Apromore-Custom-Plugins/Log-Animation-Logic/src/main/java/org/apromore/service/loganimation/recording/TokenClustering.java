package org.apromore.service.loganimation.recording;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.eclipse.collections.api.map.primitive.MutableIntObjectMap;
import org.eclipse.collections.impl.factory.primitive.IntObjectMaps;

public class TokenClustering {
    private List<MutableIntObjectMap<List<TokenCluster>>> tokenClusters = new ArrayList<>();
    
    private TokenClustering(int numberOfLogs) {
        for (int i=0; i < numberOfLogs; i++) {
            tokenClusters.add(IntObjectMaps.mutable.empty());
        }
    }
    
    private boolean isValidLog(int logIndex) {
        return logIndex >= 0 && logIndex < tokenClusters.size();
    }
    
    private boolean isValidLogElement(int logIndex, int elementIndex) {
        return isValidLog(logIndex) && tokenClusters.get(logIndex).containsKey(elementIndex);
    }
    
    public static TokenClustering of (int numberOfLogs) {
        return new TokenClustering(numberOfLogs);
    }
    
    public void addCluster(int logIndex, int elementIndex, TokenCluster cluster) {
        if (!isValidLog(logIndex)) return;
        if (!tokenClusters.get(logIndex).containsKey(elementIndex)) tokenClusters.get(logIndex).put(elementIndex, new ArrayList<>());
        tokenClusters.get(logIndex).get(elementIndex).add(cluster);
    }
    
    public void insertClusterAt(int logIndex, int elementIndex, int position, TokenCluster insertCluster) {
        if (!isValidLogElement(logIndex, elementIndex)) return;
        tokenClusters.get(logIndex).get(elementIndex).add(position, insertCluster);
    }
    
    public Set<TokenCluster> getClusters(int logIndex) {
        if (!isValidLog(logIndex)) return Collections.emptySet();
        return tokenClusters.get(logIndex).values()
                    .stream() // stream of set of clusters
                    .flatMap(setOfClusters -> setOfClusters.stream()) //map to have stream of stream of clusters, then flatten to have stream of clusters
                    .collect(Collectors.toSet());
    }
    
    // If the clusters were added in increasing order of distance, they will be returned in the same order here
    public List<TokenCluster> getClusters(int logIndex, int elementIndex) {
        if (!isValidLogElement(logIndex, elementIndex)) return Collections.emptyList();
        return Collections.unmodifiableList(tokenClusters.get(logIndex).get(elementIndex));
    }
    
    public void clear() {
        tokenClusters.forEach(logClusters -> logClusters.clear());
        tokenClusters.clear();
    }
    
    
//    public TokenClustering copy() {
//        TokenClustering copyClustering = new TokenClustering(tokenClusters.size());
//        for (int logIndex=0; logIndex<tokenClusters.size(); logIndex++) {
//            for (IntObjectPair<List<TokenCluster>> elementClustersPair : tokenClusters.get(logIndex).keyValuesView()) {
//                int elementIndex = elementClustersPair.getOne();
//                for (TokenCluster cluster : elementClustersPair.getTwo()) {
//                    copyClustering.addCluster(logIndex, elementIndex, cluster.copy());
//                }
//            }
//        }
//        return copyClustering;
//    }
}
