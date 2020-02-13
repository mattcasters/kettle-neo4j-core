package org.neo4j.kettle.shared;

import org.apache.commons.lang.StringUtils;
import org.neo4j.driver.AuthTokens;
import org.neo4j.driver.Config;
import org.neo4j.driver.Driver;
import org.neo4j.driver.GraphDatabase;
import org.neo4j.driver.Record;
import org.neo4j.driver.Result;
import org.neo4j.driver.Session;
import org.neo4j.driver.SessionConfig;
import org.neo4j.driver.Value;
import org.pentaho.di.core.Const;
import org.pentaho.di.core.encryption.Encr;
import org.pentaho.di.core.logging.LogChannel;
import org.pentaho.di.core.logging.LogChannelInterface;
import org.pentaho.di.core.row.value.ValueMetaString;
import org.pentaho.di.core.util.Utils;
import org.pentaho.di.core.variables.VariableSpace;
import org.pentaho.di.core.variables.Variables;
import org.pentaho.metastore.persist.MetaStoreAttribute;
import org.pentaho.metastore.persist.MetaStoreElementType;

import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

@MetaStoreElementType( name = "Neo4j Connection", description = "A shared connection to a Neo4j server" )
public class NeoConnection extends Variables {
  private String name;

  @MetaStoreAttribute
  private String server;

  @MetaStoreAttribute
  private String databaseName;

  @MetaStoreAttribute
  private String boltPort;

  @MetaStoreAttribute
  private String browserPort;

  @MetaStoreAttribute
  private boolean routing;

  @MetaStoreAttribute
  private String routingVariable;

  @MetaStoreAttribute
  private String routingPolicy;

  @MetaStoreAttribute
  private String username;

  @MetaStoreAttribute( password = true )
  private String password;

  @MetaStoreAttribute
  private boolean usingEncryption;

  @MetaStoreAttribute
  private List<String> manualUrls;

  @MetaStoreAttribute
  private String connectionLivenessCheckTimeout;

  @MetaStoreAttribute
  private String maxConnectionLifetime;

  @MetaStoreAttribute
  private String maxConnectionPoolSize;

  @MetaStoreAttribute
  private String connectionAcquisitionTimeout;

  @MetaStoreAttribute
  private String connectionTimeout;

  @MetaStoreAttribute
  private String maxTransactionRetryTime;

  @MetaStoreAttribute
  private boolean version4;

  public NeoConnection() {
    boltPort = "7687";
    browserPort = "7474";
    manualUrls = new ArrayList<>();
    version4 = true;
  }

  public NeoConnection( VariableSpace parent ) {
    this();
    super.initializeVariablesFrom( parent );
    usingEncryption = true;
  }

  public NeoConnection( VariableSpace parent, NeoConnection source ) {
    this( parent );
    this.name = source.name;
    this.server = source.server;
    this.boltPort = source.boltPort;
    this.browserPort = source.browserPort;
    this.routing = source.routing;
    this.routingVariable = source.routingVariable;
    this.routingPolicy = source.routingPolicy;
    this.username = source.username;
    this.password = source.password;
    this.usingEncryption = source.usingEncryption;
    this.connectionLivenessCheckTimeout = source.connectionLivenessCheckTimeout;
    this.maxConnectionLifetime = source.maxConnectionLifetime;
    this.maxConnectionPoolSize = source.maxConnectionPoolSize;
    this.connectionAcquisitionTimeout = source.connectionAcquisitionTimeout;
    this.connectionTimeout = source.connectionTimeout;
    this.maxTransactionRetryTime = source.maxTransactionRetryTime;
    this.version4 = source.version4;
  }

  @Override
  public String toString() {
    return name == null ? super.toString() : name;
  }

  @Override
  public int hashCode() {
    return name == null ? super.hashCode() : name.hashCode();
  }

  @Override
  public boolean equals( Object object ) {

    if ( object == this ) {
      return true;
    }
    if ( !( object instanceof NeoConnection ) ) {
      return false;
    }

    NeoConnection connection = (NeoConnection) object;

    return name != null && name.equalsIgnoreCase( connection.name );
  }

  /**
   * Get a Neo4j session to work with
   *
   * @param log The logchannel to log to
   * @return The Neo4j session
   */
  public Session getSession( LogChannelInterface log ) {
    Driver driver = getDriver( log );
    SessionConfig.Builder cfgBuilder = SessionConfig.builder();
    if ( StringUtils.isNotEmpty( databaseName ) ) {
      String realDatabaseName = environmentSubstitute( databaseName );
      if ( StringUtils.isNotEmpty( realDatabaseName ) ) {
        cfgBuilder.withDatabase( realDatabaseName );
      }
    }
    return driver.session( cfgBuilder.build() );
  }

  /**
   * Test this connection to Neo4j
   *
   * @throws Exception In case anything goes wrong
   */
  public void test() throws Exception {

    Session session = null;
    try {
      Driver driver = getDriver( LogChannel.GENERAL );
      SessionConfig.Builder builder = SessionConfig.builder();
      if ( StringUtils.isNotEmpty( databaseName ) ) {
        builder = builder.withDatabase( environmentSubstitute( databaseName ) );
      }
      session = driver.session( builder.build() );
      // Do something with the session otherwise it doesn't test the connection
      //
      Result result = session.run( "RETURN 0" );
      Record record = result.next();
      Value value = record.get( 0 );
      int zero = value.asInt();
      assert ( zero == 0 );
    } catch ( Exception e ) {
      throw new Exception( "Unable to connect to database '" + name + "' : " + e.getMessage(), e );
    } finally {
      if ( session != null ) {
        session.close();
      }
    }
  }

  public List<URI> getURIs() throws URISyntaxException {

    List<URI> uris = new ArrayList<>();

    if ( manualUrls != null && !manualUrls.isEmpty() ) {
      // A manual URL is specified
      //
      for ( String manualUrl : manualUrls ) {
        uris.add( new URI( manualUrl ) );
      }
    } else {
      // Construct the URIs from the entered values
      //
      List<String> serverStrings = new ArrayList<>();
      String serversString = environmentSubstitute( server );
      if ( isUsingRouting() ) {
        for ( String serverString : serversString.split( "," ) ) {
          serverStrings.add( serverString );
        }
      } else {
        serverStrings.add( serversString );
      }

      for ( String serverString : serverStrings ) {
        // Trim excess spaces from server name
        //
        String url = getUrl( Const.trim( serverString ) );
        uris.add( new URI( url ) );
      }
    }

    return uris;
  }

  public String getUrl( String hostname ) {

    /*
     * Construct the following URL:
     *
     * bolt://hostname:port
     * bolt+routing://core-server:port/?policy=MyPolicy
     */
    String url = "";
    if (isVersion4()) {
     url+="neo4j";
    } else {
      url += "bolt";
      if ( isUsingRouting() ) {
        url += "+routing";
      }
    }

    url += "://";

    // Hostname
    //
    url += hostname;

    // Port
    //
    if ( StringUtils.isNotEmpty( boltPort ) && hostname != null && !hostname.contains( ":" ) ) {
      url += ":" + environmentSubstitute( boltPort );
    }

    String routingPolicyString = environmentSubstitute( routingPolicy );
    if ( isUsingRouting() && StringUtils.isNotEmpty( routingPolicyString ) ) {
      try {
        url += "?policy=" + URLEncoder.encode( routingPolicyString, "UTF-8" );
      } catch ( Exception e ) {
        LogChannel.GENERAL.logError( "Error encoding routing policy context '" + routingPolicyString + "' in connection URL", e );
        url += "?policy=" + routingPolicyString;
      }
    }

    return url;
  }

  /**
   * Get a list of all URLs, not just the first in case of routing.
   *
   * @return
   */
  public String getUrl() {
    StringBuffer urls = new StringBuffer();
    try {
      for ( URI uri : getURIs() ) {
        if ( urls.length() > 0 ) {
          urls.append( "," );
        }
        urls.append( uri.toString() );
      }
    } catch ( Exception e ) {
      urls.append( "ERROR building URLs: " + e.getMessage() );
    }
    return urls.toString();
  }

  public Driver getDriver( LogChannelInterface log ) {

    try {
      List<URI> uris = getURIs();

      String realUsername = environmentSubstitute( username );
      String realPassword = Encr.decryptPasswordOptionallyEncrypted( environmentSubstitute( password ) );
      Config.ConfigBuilder configBuilder;
      if ( usingEncryption ) {
        configBuilder = Config.builder().withEncryption();
      } else {
        configBuilder = Config.builder().withoutEncryption();
      }
      if ( StringUtils.isNotEmpty( connectionLivenessCheckTimeout ) ) {
        long seconds = Const.toLong( environmentSubstitute( connectionLivenessCheckTimeout ), -1L );
        if ( seconds > 0 ) {
          configBuilder = configBuilder.withConnectionLivenessCheckTimeout( seconds, TimeUnit.MILLISECONDS );
        }
      }
      if ( StringUtils.isNotEmpty( maxConnectionLifetime ) ) {
        long seconds = Const.toLong( environmentSubstitute( maxConnectionLifetime ), -1L );
        if ( seconds > 0 ) {
          configBuilder = configBuilder.withMaxConnectionLifetime( seconds, TimeUnit.MILLISECONDS );
        }
      }
      if ( StringUtils.isNotEmpty( maxConnectionPoolSize ) ) {
        int size = Const.toInt( environmentSubstitute( maxConnectionPoolSize ), -1 );
        if ( size > 0 ) {
          configBuilder = configBuilder.withMaxConnectionPoolSize( size );
        }
      }
      if ( StringUtils.isNotEmpty( connectionAcquisitionTimeout ) ) {
        long seconds = Const.toLong( environmentSubstitute( connectionAcquisitionTimeout ), -1L );
        if ( seconds > 0 ) {
          configBuilder = configBuilder.withConnectionAcquisitionTimeout( seconds, TimeUnit.MILLISECONDS );
        }
      }
      if ( StringUtils.isNotEmpty( connectionTimeout ) ) {
        long seconds = Const.toLong( environmentSubstitute( connectionTimeout ), -1L );
        if ( seconds > 0 ) {
          configBuilder = configBuilder.withConnectionTimeout( seconds, TimeUnit.MILLISECONDS );
        }
      }
      if ( StringUtils.isNotEmpty( maxTransactionRetryTime ) ) {
        long seconds = Const.toLong( environmentSubstitute( maxTransactionRetryTime ), -1L );
        if ( seconds > 0 ) {
          configBuilder = configBuilder.withMaxTransactionRetryTime( seconds, TimeUnit.MILLISECONDS );
        }
      }

      Config config = configBuilder.build();

      if ( isUsingRouting() ) {
        return GraphDatabase.routingDriver( uris, AuthTokens.basic( realUsername, realPassword ), config );
      } else {
        return GraphDatabase.driver( uris.get( 0 ), AuthTokens.basic( realUsername, realPassword ), config );
      }
    } catch ( URISyntaxException e ) {
      throw new RuntimeException( "URI syntax problem, check your settings, hostnames especially.  For routing use comma separated server values.", e );
    }
  }

  public boolean isUsingRouting() {
    if ( !Utils.isEmpty( routingVariable ) ) {
      String value = environmentSubstitute( routingVariable );
      if ( !Utils.isEmpty( value ) ) {
        return ValueMetaString.convertStringToBoolean( value );
      }
    }
    return routing;
  }

  /**
   * Gets name
   *
   * @return value of name
   */
  public String getName() {
    return name;
  }

  /**
   * @param name The name to set
   */
  public void setName( String name ) {
    this.name = name;
  }

  /**
   * Gets server
   *
   * @return value of server
   */
  public String getServer() {
    return server;
  }

  /**
   * @param server The server to set
   */
  public void setServer( String server ) {
    this.server = server;
  }

  /**
   * Gets databaseName
   *
   * @return value of databaseName
   */
  public String getDatabaseName() {
    return databaseName;
  }

  /**
   * @param databaseName The databaseName to set
   */
  public void setDatabaseName( String databaseName ) {
    this.databaseName = databaseName;
  }

  /**
   * Gets boltPort
   *
   * @return value of boltPort
   */
  public String getBoltPort() {
    return boltPort;
  }

  /**
   * @param boltPort The boltPort to set
   */
  public void setBoltPort( String boltPort ) {
    this.boltPort = boltPort;
  }

  /**
   * Gets browserPort
   *
   * @return value of browserPort
   */
  public String getBrowserPort() {
    return browserPort;
  }

  /**
   * @param browserPort The browserPort to set
   */
  public void setBrowserPort( String browserPort ) {
    this.browserPort = browserPort;
  }

  /**
   * Gets routing
   *
   * @return value of routing
   */
  public boolean isRouting() {
    return routing;
  }

  /**
   * @param routing The routing to set
   */
  public void setRouting( boolean routing ) {
    this.routing = routing;
  }

  /**
   * Gets routingVariable
   *
   * @return value of routingVariable
   */
  public String getRoutingVariable() {
    return routingVariable;
  }

  /**
   * @param routingVariable The routingVariable to set
   */
  public void setRoutingVariable( String routingVariable ) {
    this.routingVariable = routingVariable;
  }

  /**
   * Gets routingPolicy
   *
   * @return value of routingPolicy
   */
  public String getRoutingPolicy() {
    return routingPolicy;
  }

  /**
   * @param routingPolicy The routingPolicy to set
   */
  public void setRoutingPolicy( String routingPolicy ) {
    this.routingPolicy = routingPolicy;
  }

  /**
   * Gets username
   *
   * @return value of username
   */
  public String getUsername() {
    return username;
  }

  /**
   * @param username The username to set
   */
  public void setUsername( String username ) {
    this.username = username;
  }

  /**
   * Gets password
   *
   * @return value of password
   */
  public String getPassword() {
    return password;
  }

  /**
   * @param password The password to set
   */
  public void setPassword( String password ) {
    this.password = password;
  }

  /**
   * Gets usingEncryption
   *
   * @return value of usingEncryption
   */
  public boolean isUsingEncryption() {
    return usingEncryption;
  }

  /**
   * @param usingEncryption The usingEncryption to set
   */
  public void setUsingEncryption( boolean usingEncryption ) {
    this.usingEncryption = usingEncryption;
  }

  /**
   * Gets manualUrls
   *
   * @return value of manualUrls
   */
  public List<String> getManualUrls() {
    return manualUrls;
  }

  /**
   * @param manualUrls The manualUrls to set
   */
  public void setManualUrls( List<String> manualUrls ) {
    this.manualUrls = manualUrls;
  }

  /**
   * Gets connectionLivenessCheckTimeout
   *
   * @return value of connectionLivenessCheckTimeout
   */
  public String getConnectionLivenessCheckTimeout() {
    return connectionLivenessCheckTimeout;
  }

  /**
   * @param connectionLivenessCheckTimeout The connectionLivenessCheckTimeout to set
   */
  public void setConnectionLivenessCheckTimeout( String connectionLivenessCheckTimeout ) {
    this.connectionLivenessCheckTimeout = connectionLivenessCheckTimeout;
  }

  /**
   * Gets maxConnectionLifetime
   *
   * @return value of maxConnectionLifetime
   */
  public String getMaxConnectionLifetime() {
    return maxConnectionLifetime;
  }

  /**
   * @param maxConnectionLifetime The maxConnectionLifetime to set
   */
  public void setMaxConnectionLifetime( String maxConnectionLifetime ) {
    this.maxConnectionLifetime = maxConnectionLifetime;
  }

  /**
   * Gets maxConnectionPoolSize
   *
   * @return value of maxConnectionPoolSize
   */
  public String getMaxConnectionPoolSize() {
    return maxConnectionPoolSize;
  }

  /**
   * @param maxConnectionPoolSize The maxConnectionPoolSize to set
   */
  public void setMaxConnectionPoolSize( String maxConnectionPoolSize ) {
    this.maxConnectionPoolSize = maxConnectionPoolSize;
  }

  /**
   * Gets connectionAcquisitionTimeout
   *
   * @return value of connectionAcquisitionTimeout
   */
  public String getConnectionAcquisitionTimeout() {
    return connectionAcquisitionTimeout;
  }

  /**
   * @param connectionAcquisitionTimeout The connectionAcquisitionTimeout to set
   */
  public void setConnectionAcquisitionTimeout( String connectionAcquisitionTimeout ) {
    this.connectionAcquisitionTimeout = connectionAcquisitionTimeout;
  }

  /**
   * Gets connectionTimeout
   *
   * @return value of connectionTimeout
   */
  public String getConnectionTimeout() {
    return connectionTimeout;
  }

  /**
   * @param connectionTimeout The connectionTimeout to set
   */
  public void setConnectionTimeout( String connectionTimeout ) {
    this.connectionTimeout = connectionTimeout;
  }

  /**
   * Gets maxTransactionRetryTime
   *
   * @return value of maxTransactionRetryTime
   */
  public String getMaxTransactionRetryTime() {
    return maxTransactionRetryTime;
  }

  /**
   * @param maxTransactionRetryTime The maxTransactionRetryTime to set
   */
  public void setMaxTransactionRetryTime( String maxTransactionRetryTime ) {
    this.maxTransactionRetryTime = maxTransactionRetryTime;
  }

  /**
   * Gets version4
   *
   * @return value of version4
   */
  public boolean isVersion4() {
    return version4;
  }

  /**
   * @param version4 The version4 to set
   */
  public void setVersion4( boolean version4 ) {
    this.version4 = version4;
  }
}
