/*
    Copyright (C) 2014 Infinite Automation Systems Inc. All rights reserved.
    @author Matthew Lohbihler
 */
package com.serotonin.m2m2.web.dwr.emport;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.serotonin.json.JsonReader;
import com.serotonin.json.type.JsonArray;
import com.serotonin.json.type.JsonObject;
import com.serotonin.json.type.JsonValue;
import com.serotonin.m2m2.Common;
import com.serotonin.m2m2.db.dao.DataPointDao;
import com.serotonin.m2m2.db.dao.SystemSettingsDao;
import com.serotonin.m2m2.i18n.ProcessResult;
import com.serotonin.m2m2.i18n.Translations;
import com.serotonin.m2m2.module.EmportDefinition;
import com.serotonin.m2m2.module.ModuleRegistry;
import com.serotonin.m2m2.util.BackgroundContext;
import com.serotonin.m2m2.util.timeout.ProgressiveTask;
import com.serotonin.m2m2.vo.User;
import com.serotonin.m2m2.vo.hierarchy.PointFolder;
import com.serotonin.m2m2.web.dwr.EmportDwr;
import com.serotonin.m2m2.web.dwr.emport.importers.DataPointImporter;
import com.serotonin.m2m2.web.dwr.emport.importers.DataPointSummaryPathPair;
import com.serotonin.m2m2.web.dwr.emport.importers.DataSourceImporter;
import com.serotonin.m2m2.web.dwr.emport.importers.EventDetectorImporter;
import com.serotonin.m2m2.web.dwr.emport.importers.EventHandlerImporter;
import com.serotonin.m2m2.web.dwr.emport.importers.JsonDataImporter;
import com.serotonin.m2m2.web.dwr.emport.importers.MailingListImporter;
import com.serotonin.m2m2.web.dwr.emport.importers.PointHierarchyImporter;
import com.serotonin.m2m2.web.dwr.emport.importers.PublisherImporter;
import com.serotonin.m2m2.web.dwr.emport.importers.SystemSettingsImporter;
import com.serotonin.m2m2.web.dwr.emport.importers.TemplateImporter;
import com.serotonin.m2m2.web.dwr.emport.importers.UserImporter;
import com.serotonin.m2m2.web.dwr.emport.importers.VirtualSerialPortImporter;

/**
 * @author Matthew Lohbihler
 */
public class ImportTask extends ProgressiveTask {
	
	private static Log LOG = LogFactory.getLog(ImportTask.class);
	
    protected final ImportContext importContext;
    protected final PointHierarchyImporter hierarchyImporter;
    private final User user;

    protected final List<Importer> importers = new ArrayList<Importer>();
    protected final List<ImportItem> importItems = new ArrayList<ImportItem>();
    protected final List<DataPointSummaryPathPair> dpPathPairs = new ArrayList<DataPointSummaryPathPair>();

    public ImportTask(JsonObject root, Translations translations, User user) {
        this(root, translations, user, true);
    }
    
    /**
     * Create an ordered task that can be queue to run one after another
     * @param root
     * @param translations
     * @param user
     * @param schedule
     */
    public ImportTask(JsonObject root, Translations translations, User user, boolean schedule) {
    	super("JSON import task", "JsonImport", Common.defaultTaskQueueSize);
    	
        JsonReader reader = new JsonReader(Common.JSON_CONTEXT, root);
        this.importContext = new ImportContext(reader, new ProcessResult(), translations);
        this.user = user;

        for (JsonValue jv : nonNullList(root, EmportDwr.USERS))
            addImporter(new UserImporter(this.user, jv.toJsonObject()));
        
        for (JsonValue jv : nonNullList(root, EmportDwr.DATA_SOURCES))
            addImporter(new DataSourceImporter(jv.toJsonObject()));
        
        for (JsonValue jv : nonNullList(root, EmportDwr.DATA_POINTS))
            addImporter(new DataPointImporter(jv.toJsonObject(), dpPathPairs));
        
        JsonArray phJson = root.getJsonArray(EmportDwr.POINT_HIERARCHY);
        if(phJson != null) {
            hierarchyImporter = new PointHierarchyImporter(phJson, true);
        	addImporter(hierarchyImporter);
        } else
            hierarchyImporter = null;
        
        for (JsonValue jv : nonNullList(root, EmportDwr.MAILING_LISTS))
            addImporter(new MailingListImporter(jv.toJsonObject()));
        
        for (JsonValue jv : nonNullList(root, EmportDwr.PUBLISHERS))
            addImporter(new PublisherImporter(jv.toJsonObject()));
        
        for (JsonValue jv : nonNullList(root, EmportDwr.EVENT_HANDLERS))
            addImporter(new EventHandlerImporter(jv.toJsonObject()));
        
        JsonObject obj = root.getJsonObject(EmportDwr.SYSTEM_SETTINGS);
        if(obj != null)
            addImporter(new SystemSettingsImporter(obj));
        
        for (JsonValue jv : nonNullList(root, EmportDwr.TEMPLATES))
            addImporter(new TemplateImporter(jv.toJsonObject()));
        
        for (JsonValue jv : nonNullList(root, EmportDwr.VIRTUAL_SERIAL_PORTS))
            addImporter(new VirtualSerialPortImporter(jv.toJsonObject()));
        
        for(JsonValue jv : nonNullList(root, EmportDwr.JSON_DATA))
        	addImporter(new JsonDataImporter(jv.toJsonObject()));
        
        for (EmportDefinition def : ModuleRegistry.getDefinitions(EmportDefinition.class)) {
            ImportItem importItem = new ImportItem(def, root.get(def.getElementId()));
            importItems.add(importItem);
        }
        
        for(JsonValue jv : nonNullList(root, EmportDwr.EVENT_DETECTORS)) 
        	addImporter(new EventDetectorImporter(jv.toJsonObject()));

        if(schedule)    
            Common.backgroundProcessing.execute(this);
    }

    private List<JsonValue> nonNullList(JsonObject root, String key) {
        JsonArray arr = root.getJsonArray(key);
        if (arr == null)
            arr = new JsonArray();
        return arr;
    }

    private void addImporter(Importer importer) {
        importer.setImportContext(importContext);
        importer.setImporters(importers);
        importers.add(importer);
    }

    public ProcessResult getResponse() {
        return importContext.getResult();
    }

    protected int importerIndex;
    protected boolean importerSuccess;
    protected boolean importedItems;

    @Override
    protected void runImpl() {
        try {
            BackgroundContext.set(user);

            if (!importers.isEmpty()) {
                if (importerIndex >= importers.size()) {
                    // A run through the importers has been completed.
                    if (importerSuccess) {
                        // If there were successes with the importers and there are still more to do, run through 
                        // them again.
                        importerIndex = 0;
                        importerSuccess = false;
                    } else if(!importedItems) {
        	            try {
                            for (ImportItem importItem : importItems) {
                                if (!importItem.isComplete()) {
                                    importItem.importNext(importContext);
                                    return;
                                }
                            }
                            importedItems = true;   // We may have imported a dependency in a module
                            importerIndex = 0;
                        }
                        catch (Exception e) {
                            addException(e);
                        }
                    } else {
                        // There are importers left in the list, but there were no successful imports in the last run
                        // of the set. So, all that is left is stuff that will always fail. Copy the validation 
                        // messages to the context for each.
                    	// Run the import items.
                        for (Importer importer : importers)
                            importer.copyMessages();
                        importers.clear();
                        processDataPointPaths(hierarchyImporter, dpPathPairs);
                        completed = true;
                        return;
                    }
                }

                // Run the next importer
                Importer importer = importers.get(importerIndex);
                try {
                    importer.doImport();
                    if (importer.success()) {
                        // The import was successful. Note the success and remove the importer from the list.
                        importerSuccess = true;
                        importers.remove(importerIndex);
                    }
                    else{
                        // The import failed. Leave it in the list since the run of another importer
                        // may resolved the problem.
                        importerIndex++;
                    }
                }
                catch (Exception e) {
                    // Uh oh...
                	LOG.error(e.getMessage(),e);
                    addException(e);
                    importers.remove(importerIndex);
                }

                return;
            }

            // Run the import items.
            try {
                for (ImportItem importItem : importItems) {
                    if (!importItem.isComplete()) {
                        importItem.importNext(importContext);
                        return;
                    }
                }
                processDataPointPaths(hierarchyImporter, dpPathPairs);
                completed = true;
            }
            catch (Exception e) {
                addException(e);
            }
        }
        finally {
            BackgroundContext.remove();
        }
    }
    
    public static void processDataPointPaths(PointHierarchyImporter hierarchyImporter, List<DataPointSummaryPathPair> dpPathPairs) {
        PointFolder root;
        if(hierarchyImporter != null && hierarchyImporter.getHierarchy() != null) 
            root = hierarchyImporter.getHierarchy().getRoot();
        else
            root = DataPointDao.instance.getPointHierarchy(false).getRoot();
        String pathSeparator = SystemSettingsDao.getValue(SystemSettingsDao.HIERARCHY_PATH_SEPARATOR);
        for(DataPointSummaryPathPair dpp : dpPathPairs) {
            root.removePointRecursively(dpp.getDataPointSummary().getId());
            PointFolder starting = root;
            PointFolder previous = root;
            String[] pathParts = dpp.getPath().split(pathSeparator);
            
            if(pathParts.length == 1 && StringUtils.isBlank(pathParts[0])) { //Check if it's in the root
                root.getPoints().add(dpp.getDataPointSummary());
                continue;
            }
            
            for(String s : pathParts) {
                if(starting == null) {
                    PointFolder newFolder = new PointFolder();
                    newFolder.setName(s);
                    previous.addSubfolder(newFolder);
                    starting = newFolder;
                }
                previous = starting;
                starting = starting.getSubfolder(s);
            }
            if(starting != null)
                starting.getPoints().add(dpp.getDataPointSummary());
            else {
                PointFolder newFolder = new PointFolder();
                newFolder.setName(pathParts[pathParts.length-1]);
                previous.addSubfolder(newFolder);
                newFolder.addDataPoint(dpp.getDataPointSummary());
            }
        }
        DataPointDao.instance.savePointHierarchy(root);
    }

    private void addException(Exception e) {
        String msg = e.getMessage();
        Throwable t = e;
        while ((t = t.getCause()) != null)
            msg += ", " + importContext.getTranslations().translate("emport.causedBy") + " '" + t.getMessage() + "'";
        //We were missing NPE and others without a msg
        if(msg == null)
        	msg = e.getClass().getCanonicalName();
        importContext.getResult().addGenericMessage("common.default", msg);
    }
}
