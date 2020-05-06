package org.neo4j.kettle.model.cw;

import org.apache.commons.io.FileUtils;
import org.junit.Before;
import org.junit.Test;
import org.neo4j.kettle.model.GraphModel;
import org.neo4j.kettle.model.GraphNode;
import org.neo4j.kettle.model.GraphPresentation;
import org.neo4j.kettle.model.GraphProperty;
import org.neo4j.kettle.model.GraphRelationship;

import java.io.File;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class CypherWorkbenchLegoImporterTest {

  private GraphModel graphModel;

  @Before
  public void setUp() throws Exception {
    String jsonString = FileUtils.readFileToString( new File( "src/test/resources/lego/Lego_Model.json" ), "UTF-8" );
    graphModel = CypherWorkbenchImporter.importFromCwJson( jsonString );
    graphModel = CypherWorkbenchImporter.changeNamesToLabels( graphModel );
  }

  @Test
  public void importFromCwJson() throws Exception {
    assertEquals( "model name is not taken from CW model title", "Lego (4/20/2020, 10:25:39 AM)", graphModel.getName() );
    assertEquals( "model description is not taken from CW model description", "Imported using apoc.meta.schema from Lego Localhost (bolt://localhost:5687)", graphModel.getDescription() );

    List<GraphNode> nodes = graphModel.getNodes();
    assertEquals( "Not all nodes were imported", 7, nodes.size() );

    // Validate the Inventory node and a few properties
    //
    GraphNode inventoryNode = graphModel.findNode( "Inventory" );
    validateNode(inventoryNode, "Inventory", 2, 640, 160);
    validateProperty( inventoryNode.findProperty( "version" ), "version", "String", false, false, false, false );
    validateProperty( inventoryNode.findProperty( "id" ), "id", "String", true, true, true, true );

    // Check out the Theme node
    //
    GraphNode themeNode = graphModel.findNode( "Theme" );
    validateNode( themeNode, "Theme", 2, 1280, 280 );
    validateProperty( themeNode.findProperty( "name" ), "name", "String", false, true, false, false );
    validateProperty( themeNode.findProperty( "id" ), "id", "String", true, true, true, true );

    // Validate the Part node
    //
    GraphNode partNode = graphModel.findNode( "Part" );
    validateNode( partNode, "Part", 2, 700, 320 );
    validateProperty( partNode.findProperty( "part_num" ), "part_num", "String", true, true, true, true );
    validateProperty( partNode.findProperty( "name" ), "name", "String", false, true, false, false );

    // The Color node
    //
    GraphNode colorNode = graphModel.findNode( "Color" );
    validateNode( colorNode, "Color", 4, 620, 480 );
    validateProperty(colorNode.findProperty( "id" ), "id", "String", true, true, true, true);
    validateProperty(colorNode.findProperty( "is_trans" ), "is_trans", "String", false, false, false, false);
    validateProperty(colorNode.findProperty( "rgb" ), "rgb", "String", false, false, false, false);
    validateProperty(colorNode.findProperty( "name" ), "name", "String", false, true, false, false);

    // The Set node
    //
    GraphNode setNode = graphModel.findNode( "Set" );
    validateNode( setNode, "Set", 4, 1020, 160 );
    validateProperty(setNode.findProperty( "name" ), "name", "String", false, true, false, false);
    validateProperty(setNode.findProperty( "year" ), "year", "Integer", false, false, false, false);
    validateProperty(setNode.findProperty( "num_parts" ), "num_parts", "Integer", false, false, false, false);
    validateProperty(setNode.findProperty( "set_num" ), "set_num", "String", true, true, true, true);

    // The Part Category
    //
    GraphNode partCategoryNode = graphModel.findNode( "PartCategory" );
    validateNode( partCategoryNode, "PartCategory", 2, 960, 320 );
    validateProperty(partCategoryNode.findProperty( "id" ), "id", "String", true, true, true, true);
    validateProperty(partCategoryNode.findProperty( "name" ), "name", "String", false, false, false, false);

    // The Part Category
    //
    GraphNode partInventoryNode = graphModel.findNode( "InventoryPart" );
    validateNode( partInventoryNode, "InventoryPart", 5, 400, 320 );
    validateProperty(partInventoryNode.findProperty( "color_id" ), "color_id", "String", false, true, false, false);
    validateProperty(partInventoryNode.findProperty( "inventory_id" ), "inventory_id", "String", false, true, false, false);
    validateProperty(partInventoryNode.findProperty( "part_num" ), "part_num", "String", false, true, false, false);
    validateProperty(partInventoryNode.findProperty( "is_spare" ), "is_spare", "Boolean", false, true, false, false);
    validateProperty(partInventoryNode.findProperty( "quantity" ), "quantity", "String", false, false, false, false);




    // Validate the relationships
    //
    assertEquals( 7, graphModel.getRelationships().size() );
    validateRelationship("HAS_PART_CATEGORY", "Part", "PartCategory", 0);
    validateRelationship("INVENTORY_FOR", "Inventory", "Set", 1);
    GraphProperty setQuantityProperty = graphModel.findRelationship( "INVENTORY_FOR" ).findProperty( "setQuantity" );
    validateProperty( setQuantityProperty, "setQuantity", "Integer", false, false, false, false );
    validateRelationship("HAS_THEME", "Set", "Theme", 0);
    validateRelationship("HAS_PARENT_THEME", "Theme", "Theme", 0);
    validateRelationship("HAS_COLOR", "InventoryPart", "Color", 0);
    validateRelationship("ASSOCIATED_INVENTORY", "InventoryPart", "Inventory", 0);
    validateRelationship("FOR_PART", "InventoryPart", "Part", 0);

  }

  private void validateRelationship( String name, String startNode, String endNode, int nrProperties ) {
    GraphRelationship relationship = graphModel.findRelationship( name );
    assertNotNull( relationship );
    assertEquals( relationship.getNodeSource(), startNode );
    assertEquals( relationship.getNodeTarget(), endNode );
    assertEquals( nrProperties, relationship.getProperties().size() );
  }

  private void validateNode( GraphNode node, String name, int nrProperties, int x, int y ) {
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

  private void validateProperty( GraphProperty property, String name, String type, boolean primary, boolean indexed, boolean mandatory, boolean unique ) {
    assertNotNull("Property "+name+" must exist", property);
    assertEquals(property.getName(), name);
    assertEquals("Property "+name+" has the wrong type", property.getType().name(), type);
    assertEquals("Property "+name+" has wrong primary flag", property.isPrimary(), primary);
    assertEquals("Property "+name+" has wrong indexed flag", property.isIndexed(), indexed);
    assertEquals("Property "+name+" has wrong mandatory flag", property.isMandatory(), mandatory);
    assertEquals("Property "+name+" has wrong unique flag", property.isUnique(), unique);
  }


}
