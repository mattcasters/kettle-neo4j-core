package org.neo4j.kettle.model.cw;

import org.apache.commons.io.FileUtils;
import org.junit.Before;
import org.junit.Test;
import org.neo4j.kettle.model.GraphModel;
import org.neo4j.kettle.model.GraphNode;
import org.neo4j.kettle.model.GraphPresentation;
import org.neo4j.kettle.model.GraphProperty;
import org.neo4j.kettle.model.GraphRelationship;
import org.pentaho.di.core.exception.KettleException;

import java.io.File;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

public class CypherWorkbenchBaseImporterTest {

  protected GraphModel graphModel;

  protected void validateRelationship( String name, String startNode, String endNode, int nrProperties ) {
    GraphRelationship relationship = graphModel.findRelationship( name );
    assertNotNull( relationship );
    assertEquals( relationship.getNodeSource(), startNode );
    assertEquals( relationship.getNodeTarget(), endNode );
    assertEquals( nrProperties, relationship.getProperties().size() );
  }

  protected void validateNode( GraphNode node, String name, int nrProperties, int x, int y ) {
    assertNotNull( node );
    assertEquals( name, node.getName() );
    assertEquals( 1, node.getLabels().size() );
    assertEquals( name, node.getLabels().get( 0 ) );

    GraphPresentation nodePresentation = node.getPresentation();
    assertNotNull( nodePresentation );
    assertEquals( "Node "+name+" has the wrong x location", x, nodePresentation.getX() );
    assertEquals( "Node "+name+" has the wrong x location", y, nodePresentation.getY() );
    assertEquals( "Node "+name+" has the wrong number or properties", nrProperties, node.getProperties().size() );
  }

  protected void validateProperty( GraphProperty property, String name, String type, boolean primary, boolean indexed, boolean mandatory, boolean unique ) {
    assertNotNull("Property "+name+" must exist", property);
    assertEquals(property.getName(), name);
    assertEquals("Property "+name+" has the wrong type", property.getType().name(), type);
    assertEquals("Property "+name+" has wrong primary flag", property.isPrimary(), primary);
    assertEquals("Property "+name+" has wrong indexed flag", property.isIndexed(), indexed);
    assertEquals("Property "+name+" has wrong mandatory flag", property.isMandatory(), mandatory);
    assertEquals("Property "+name+" has wrong unique flag", property.isUnique(), unique);
  }
}
