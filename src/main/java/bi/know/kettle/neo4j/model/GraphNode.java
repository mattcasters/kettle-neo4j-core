package bi.know.kettle.neo4j.model;

import org.neo4j.kettle.model.GraphProperty;

import java.util.List;

public class GraphNode extends org.neo4j.kettle.model.GraphNode {
  public GraphNode() {
    super();
  }

  public GraphNode( String name, String description, List<String> labels, List<GraphProperty> properties ) {
    super( name, description, labels, properties );
  }

  public GraphNode( org.neo4j.kettle.model.GraphNode graphNode ) {
    super( graphNode );
  }
}
