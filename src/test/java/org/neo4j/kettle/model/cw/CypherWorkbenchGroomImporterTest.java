package org.neo4j.kettle.model.cw;

import org.apache.commons.io.FileUtils;
import org.junit.Before;
import org.junit.Test;
import org.neo4j.kettle.model.GraphNode;
import org.neo4j.kettle.model.GraphProperty;
import org.neo4j.kettle.model.GraphRelationship;

import java.io.File;
import java.util.List;

import static org.junit.Assert.assertEquals;

public class CypherWorkbenchGroomImporterTest extends CypherWorkbenchBaseImporterTest {

  @Before
  public void setUp() throws Exception {
    String jsonString = FileUtils.readFileToString( new File( "src/test/resources/groom/groom.json" ), "UTF-8" );
    graphModel = CypherWorkbenchImporter.importFromCwJson( jsonString );
    graphModel = CypherWorkbenchImporter.changeNamesToLabels( graphModel );
  }

  @Test
  public void importFromCwJson() {
    assertEquals( "model name is not taken from CW model title", "Groom", graphModel.getName() );
    assertEquals( "model description is not taken from CW model description", "The Doom Graph Model", graphModel.getDescription() );

    List<GraphNode> nodes = graphModel.getNodes();
    assertEquals( "Not all nodes were imported", 9, nodes.size() );

    List<GraphRelationship> relationships = graphModel.getRelationships();
    assertEquals( "Not all relationships were imported", 20, relationships.size() );

    // TODO: validate data as well

  }
}
