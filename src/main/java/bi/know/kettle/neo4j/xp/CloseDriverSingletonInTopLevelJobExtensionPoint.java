package bi.know.kettle.neo4j.xp;

import bi.know.kettle.neo4j.core.Neo4jDefaults;
import bi.know.kettle.neo4j.shared.DriverSingleton;
import org.apache.commons.lang.StringUtils;
import org.pentaho.di.core.exception.KettleException;
import org.pentaho.di.core.extension.ExtensionPoint;
import org.pentaho.di.core.extension.ExtensionPointInterface;
import org.pentaho.di.core.logging.LogChannelInterface;
import org.pentaho.di.job.Job;
import org.pentaho.di.job.JobAdapter;
import org.pentaho.di.metastore.MetaStoreConst;
import org.pentaho.metastore.api.IMetaStore;
import org.pentaho.metastore.stores.delegate.DelegatingMetaStore;

@ExtensionPoint(
  id = "CloseDriverSingletonInTopLevelJobExtensionPoint",
  extensionPointId = "JobStart",
  description = "Close the Neo4j driver singleton at the end of a top level job"
)
public class CloseDriverSingletonInTopLevelJobExtensionPoint implements ExtensionPointInterface {

  @Override public void callExtensionPoint( LogChannelInterface log, Object object ) throws KettleException {

    if ( !( object instanceof Job ) ) {
      return; // not for us
    }

    Job job = (Job) object;

    job.addJobListener( new JobAdapter() {
      @Override public void jobFinished( Job job ) throws KettleException {
        // Only close if we're in a top level job and not running on a container
        //
        if (job.getContainerObjectId()==null && job.getParentJob()==null && job.getParentTrans()==null) {

          String cleanup = job.getVariable( Neo4jDefaults.VARIABLE_NEO4J_CLEANUP_DRIVERS );
          if (!StringUtils.isEmpty( cleanup ) && ( "Y".equalsIgnoreCase( cleanup  ) || "Yes".equalsIgnoreCase( cleanup ) || "TRUE".equalsIgnoreCase( cleanup ))  ) {
            DriverSingleton.closeAll();
          }
        }
      }
    } );
  }
}
