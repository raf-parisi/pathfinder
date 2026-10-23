package pathfinder.model;

public record SearchStep(
        Node node,
        NodeState state
) {}