/*
 *
 *  *  Copyright 2014 Orient Technologies LTD (info(at)orientechnologies.com)
 *  *
 *  *  Licensed under the Apache License, Version 2.0 (the "License");
 *  *  you may not use this file except in compliance with the License.
 *  *  You may obtain a copy of the License at
 *  *
 *  *       http://www.apache.org/licenses/LICENSE-2.0
 *  *
 *  *  Unless required by applicable law or agreed to in writing, software
 *  *  distributed under the License is distributed on an "AS IS" BASIS,
 *  *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  *  See the License for the specific language governing permissions and
 *  *  limitations under the License.
 *  *
 *  * For more information: http://www.orientechnologies.com
 *
 */

package com.orientechnologies.common.profiler;

import com.orientechnologies.common.concur.resource.OSharedResourceAbstract;
import com.orientechnologies.common.io.OFileUtils;
import com.orientechnologies.common.log.OLogManager;
import com.orientechnologies.common.util.OPair;
import com.orientechnologies.orient.core.Orient;
import com.orientechnologies.orient.core.config.OGlobalConfiguration;
import com.orientechnologies.orient.core.index.hashindex.local.cache.ODiskCache;
import com.orientechnologies.orient.core.storage.OStorage;
import com.orientechnologies.orient.core.storage.impl.local.paginated.OLocalPaginatedStorage;

import java.io.PrintStream;
import java.lang.management.ManagementFactory;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentHashMap;

import javax.management.MBeanServer;
import javax.management.ObjectName;

public abstract class OAbstractProfiler extends OSharedResourceAbstract implements OProfilerMBean {

  protected final Map<String, OProfilerHookValue>        hooks         = new ConcurrentHashMap<String, OProfilerHookValue>();
  protected final ConcurrentHashMap<String, String>      dictionary    = new ConcurrentHashMap<String, String>();
  protected final ConcurrentHashMap<String, METRIC_TYPE> types         = new ConcurrentHashMap<String, METRIC_TYPE>();
  protected long                                         recordingFrom = -1;

  public interface OProfilerHookValue {
    public Object getValue();
  }

  public OAbstractProfiler() {
    installMemoryChecker();
  }

  public OAbstractProfiler(final OAbstractProfiler profiler) {
    hooks.putAll(profiler.hooks);
    dictionary.putAll(profiler.dictionary);
    types.putAll(profiler.types);

    installMemoryChecker();
  }

  public static void dumpEnvironment(final PrintStream out) {
    final Runtime runtime = Runtime.getRuntime();

    int stgs = 0;
    long diskCacheUsed = 0;
    long diskCacheTotal = 0;
    for (OStorage stg : Orient.instance().getStorages()) {
      if (stg instanceof OLocalPaginatedStorage) {
        diskCacheUsed += ((OLocalPaginatedStorage) stg).getDiskCache().getUsedMemory();
        diskCacheTotal += OGlobalConfiguration.DISK_CACHE_SIZE.getValueAsLong() * 1024 * 1024;
        stgs++;
      }
    }
    try
	{
		MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();
		ObjectName osMBeanName = ObjectName.getInstance(ManagementFactory.OPERATING_SYSTEM_MXBEAN_NAME);
		if(mbs.isInstanceOf(osMBeanName, "com.sun.management.OperatingSystemMXBean"))
		{
		  final long osTotalMem = ((Number)mbs.getAttribute(osMBeanName, "TotalPhysicalMemorySize")).longValue();
		  final long osUsedMem = ((Number)mbs.getAttribute(osMBeanName, "FreePhysicalMemorySize")).longValue();

		  out.printf("OrientDB Memory profiler: Heap=%s of %s - DiskCache (%s dbs)=%s of %s - OS=%s of %s\n",
		      OFileUtils.getSizeAsString(runtime.totalMemory() - runtime.freeMemory()),
		      OFileUtils.getSizeAsString(runtime.maxMemory()), stgs, OFileUtils.getSizeAsString(diskCacheUsed),
		      OFileUtils.getSizeAsString(diskCacheTotal), OFileUtils.getSizeAsString(osUsedMem), OFileUtils.getSizeAsString(osTotalMem));
		  return;
		}
	} catch (Exception e)
	{
		// Nothing to do. Proceed with default output
	} 
    
      out.printf("OrientDB Memory profiler: Heap=%s of %s - DiskCache (%s dbs)=%s of %s\n",
          OFileUtils.getSizeAsString(runtime.totalMemory() - runtime.freeMemory()),
          OFileUtils.getSizeAsString(runtime.maxMemory()), stgs, OFileUtils.getSizeAsString(diskCacheUsed),
          OFileUtils.getSizeAsString(diskCacheTotal));
  }

  public void shutdown() {
    stopRecording();
  }

  public boolean startRecording() {
    if (isRecording())
      return false;

    recordingFrom = System.currentTimeMillis();
    return true;
  }

  public boolean stopRecording() {
    if (!isRecording())
      return false;

    recordingFrom = -1;
    return true;
  }

  public boolean isRecording() {
    return recordingFrom > -1;
  }

  public void updateCounter(final String iStatName, final String iDescription, final long iPlus) {
    updateCounter(iStatName, iDescription, iPlus, iStatName);
  }

  @Override
  public String getName() {
    return "profiler";
  }

  @Override
  public void startup() {
    startRecording();
  }

  @Override
  public String dump() {
    return null;
  }

  @Override
  public String dumpCounters() {
    return null;
  }

  @Override
  public OProfilerEntry getChrono(String string) {
    return null;
  }

  @Override
  public long startChrono() {
    return 0;
  }

  @Override
  public long stopChrono(String iName, String iDescription, long iStartTime) {
    return 0;
  }

  @Override
  public long stopChrono(String iName, String iDescription, long iStartTime, String iDictionary) {
    return 0;
  }

  @Override
  public String dumpChronos() {
    return null;
  }

  @Override
  public String[] getCountersAsString() {
    return null;
  }

  @Override
  public String[] getChronosAsString() {
    return null;
  }

  @Override
  public Date getLastReset() {
    return null;
  }

  @Override
  public void setAutoDump(int iNewValue) {
  }

  @Override
  public String metadataToJSON() {
    return null;
  }

  @Override
  public Map<String, OPair<String, METRIC_TYPE>> getMetadata() {
    final Map<String, OPair<String, METRIC_TYPE>> metadata = new HashMap<String, OPair<String, METRIC_TYPE>>();
    for (Entry<String, String> entry : dictionary.entrySet())
      metadata.put(entry.getKey(), new OPair<String, METRIC_TYPE>(entry.getValue(), types.get(entry.getKey())));
    return metadata;
  }

  public void registerHookValue(final String iName, final String iDescription, final METRIC_TYPE iType,
      final OProfilerHookValue iHookValue) {
    registerHookValue(iName, iDescription, iType, iHookValue, iName);
  }

  public void registerHookValue(final String iName, final String iDescription, final METRIC_TYPE iType,
      final OProfilerHookValue iHookValue, final String iMetadataName) {
    if (iName != null) {
      unregisterHookValue(iName);
      updateMetadata(iMetadataName, iDescription, iType);
      hooks.put(iName, iHookValue);
    }
  }

  @Override
  public void unregisterHookValue(final String iName) {
    if (iName != null)
      hooks.remove(iName);
  }

  @Override
  public String getSystemMetric(final String iMetricName) {
    final StringBuilder buffer = new StringBuilder("system.".length() + iMetricName.length() + 1);
    buffer.append("system.");
    buffer.append(iMetricName);
    return buffer.toString();
  }

  @Override
  public String getProcessMetric(final String iMetricName) {
    final StringBuilder buffer = new StringBuilder("process.".length() + iMetricName.length() + 1);
    buffer.append("process.");
    buffer.append(iMetricName);
    return buffer.toString();
  }

  @Override
  public String getDatabaseMetric(final String iDatabaseName, final String iMetricName) {
    final StringBuilder buffer = new StringBuilder(128);
    buffer.append("db.");
    buffer.append(iDatabaseName != null ? iDatabaseName : "*");
    buffer.append('.');
    buffer.append(iMetricName);
    return buffer.toString();
  }

  @Override
  public String toJSON(String command, final String iPar1) {
    return null;
  }

  protected void installMemoryChecker() {
    Orient.instance().getTimer().schedule(new TimerTask() {
      @Override
      public void run() {
        final java.lang.management.OperatingSystemMXBean mxBean = ManagementFactory.getOperatingSystemMXBean();
        final long jvmTotMemory = Runtime.getRuntime().totalMemory();
        final long jvmMaxMemory = Runtime.getRuntime().maxMemory();

        for (OStorage s : Orient.instance().getStorages()) {
          if (s instanceof OLocalPaginatedStorage) {
            final ODiskCache dk = ((OLocalPaginatedStorage) s).getDiskCache();
            if (dk == null)
              // NOT YET READY
              continue;

            final long totalDiskCacheUsedMemory = dk.getUsedMemory() / OFileUtils.MEGABYTE;
            final long maxDiskCacheUsedMemory = OGlobalConfiguration.DISK_CACHE_SIZE.getValueAsLong();

            if (jvmTotMemory < jvmMaxMemory && (totalDiskCacheUsedMemory * 120 / 100) > maxDiskCacheUsedMemory) {

              final long suggestedMaxHeap = jvmTotMemory * 120 / 100;
              final long suggestedDiskCache = OGlobalConfiguration.DISK_CACHE_SIZE.getValueAsLong()
                  + (jvmMaxMemory - suggestedMaxHeap) / OFileUtils.MEGABYTE;

              OLogManager
                  .instance()
                  .info(
                      this,
                      "Database '%s' uses %,dMB/%,dMB of DISKCACHE memory, while Heap is not completely used (usedHeap=%dMB maxHeap=%dMB). To improve performance set maxHeap to %dMB and DISKCACHE to %dMB",
                      s.getName(), totalDiskCacheUsedMemory, maxDiskCacheUsedMemory, jvmTotMemory / OFileUtils.MEGABYTE,
                      jvmMaxMemory / OFileUtils.MEGABYTE, suggestedMaxHeap / OFileUtils.MEGABYTE, suggestedDiskCache);

              OLogManager
                  .instance()
                  .info(
                      this,
                      "-> Open server.sh (or server.bat on Windows) and change the following variables: 1) MAXHEAP=-Xmx%dM 2) MAXDISKCACHE=%d",
                      suggestedMaxHeap / OFileUtils.MEGABYTE, suggestedDiskCache);
            }

          }
        }
      }
    }, 5000, 5000);
  }

  /**
   * Updates the metric metadata.
   */
  protected void updateMetadata(final String iName, final String iDescription, final METRIC_TYPE iType) {
    if (iDescription != null && dictionary.putIfAbsent(iName, iDescription) == null)
      types.put(iName, iType);
  }
}