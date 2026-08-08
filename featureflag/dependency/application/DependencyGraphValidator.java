package com.company.featureflag.dependency.application;

import com.company.featureflag.dependency.domain.FeatureDependency;
import org.springframework.stereotype.Component;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Detects whether adding the edge {@code featureId -> dependsOnFeatureId}
 * would create a cycle (spec §14: reject A -> B -> A). Runs a DFS from
 * {@code dependsOnFeatureId} over the existing dependency edges: if that
 * traversal can already reach {@code featureId}, the new edge would close
 * a loop.
 */
@Component
public class DependencyGraphValidator {

    public boolean wouldCreateCycle(UUID featureId, UUID dependsOnFeatureId, List<FeatureDependency> existingEdges) {
        if (featureId.equals(dependsOnFeatureId)) {
            return true;
        }
        Map<UUID, List<UUID>> adjacency = existingEdges.stream()
                .collect(Collectors.groupingBy(FeatureDependency::getFeatureId,
                        Collectors.mapping(FeatureDependency::getDependsOnFeatureId, Collectors.toList())));

        Deque<UUID> stack = new ArrayDeque<>();
        Set<UUID> visited = new HashSet<>();
        stack.push(dependsOnFeatureId);

        while (!stack.isEmpty()) {
            UUID current = stack.pop();
            if (current.equals(featureId)) {
                return true;
            }
            if (!visited.add(current)) {
                continue;
            }
            for (UUID next : adjacency.getOrDefault(current, List.of())) {
                stack.push(next);
            }
        }
        return false;
    }
}
