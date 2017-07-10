/**
 * Copyright 2017, Computer Sciences Corporation. All rights reserved.
 * 
 * This computer program is protected by copyright law and international treaties.
 * Unauthorized reproduction or distribution of this program, or any portion of it,
 * may result in severe civil and criminal penalties, and will be prosecuted to the
 * maximum extent possible under the law.
 */
package com.csc.insrules.rulesInitiate;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import com.csc.insrules.cacheHelper.CacheHelper;
import com.csc.insrules.xmlutils.*;
import com.csc.insrules.dpapihelper.*;

public class RulesSysInfo
{
  //****************************************************************************************
  // FUNCTION: GetRuleSysParm                                                              *
  //                                                                                       *
  // This function accepts a unique node name searches the xml system document and returns *
  // the value of the node to the calling routing.                                         *
  //                                                                                       *
  //****************************************************************************************

  private  boolean ENTROPYKEY = true; 
  private String ENV_ID = "";

  public String getENV_ID() {
	return ENV_ID;
}

public void setENV_ID(String eNV_ID) {
	ENV_ID = eNV_ID;
}

  //Start IRBA-295 - modified the AddEventSettings functionality
  //Create a new Event and save
  public final XmlNode AddEventSettings(String eventName, String version, String model)
  {
	XmlDocument objXMLConfigurationDocument;
	String ruleLocation;
	XmlNode objEventSetting;
	XmlNode objTempNode;
	String ENVID = getENV_ID();
	CacheHelper cache = new CacheHelper();
	
	//reload RulesSysInfo.xml from runtime folder
	objXMLConfigurationDocument =  reloadModel(false);
	
	if (GetRuleSysParm("RUNTIMEFOLDER").length() == 0)
	{
	  ruleLocation = modRuntimeCommon.GetRuntimeDirectory() + "\\";
	}
	else
	{
	  ruleLocation = "%%RUNTIMEFOLDER%%\\";
	}	
	
	//Create EventSettings Node in RulesSysInfo.xml
	objEventSetting = objXMLConfigurationDocument.CreateElement("EventSettings");
	
	//Create Event Node
	objTempNode = objXMLConfigurationDocument.CreateElement("Event");	
	objTempNode.setTextContent(eventName);
	objEventSetting.AppendChild(objTempNode);	
	
	if (version.length() != 0)
	{
	  //Create Version node
	  objTempNode = objXMLConfigurationDocument.CreateElement("Version");	  
	  objTempNode.setTextContent(version);
	  objEventSetting.AppendChild(objTempNode);
	}


	String[] stringArray = model.split("[;]");

	for (short i = 0; i < stringArray.length; i++)
	{
	  objTempNode = objXMLConfigurationDocument.CreateElement("ModelLocation");
	  objTempNode.setTextContent(ruleLocation + "RulesModels\\" + stringArray[i]);
	  objEventSetting.AppendChild(objTempNode);
	}

	objXMLConfigurationDocument.SelectSingleNode("//RulesSystemConfiguration").AppendChild(objEventSetting);
	SaveRuleSysInfo(objXMLConfigurationDocument);
	
	return objEventSetting;
  }
  //End IRBA-295 
  
  public final XmlNode AddModeltoEvent(String EventName, String Version, String Model)
  {
	XmlDocument objXMLConfigurationDocument = new XmlDocument();
	String RuleLocation = null;
	short i = 0;
	XmlNode objXMLEventNode = null;
	//Start IRBA-295 
	String ENVID = getENV_ID();
	CacheHelper cache = new CacheHelper();	
	
	objXMLConfigurationDocument = reloadModel(false); //reload RulesSysInfo.xml from runtime folder
	//End IRBA-295
	 
	// Locate the node based upon Event and Version
	if (Version != null && Version.length() > 0)
	{
	  objXMLEventNode = objXMLConfigurationDocument.SelectSingleNode("//RulesSystemConfiguration/EventSettings[(@Event='" + EventName + "' and @Version='" + Version + "') or (@Event='" + EventName + "' and @Version>'" + Version + "') or (Event='" + EventName + "' and @Version='" + Version + "') or (Event='" + EventName + "' and @Version>'" + Version + "')]");
	}

	// If previous lookup was not performed or did not find a match then lookup without the version
	if (objXMLEventNode == null)
	{
	  objXMLEventNode = objXMLConfigurationDocument.SelectSingleNode("RulesSystemConfiguration/EventSettings[(@Event='" + EventName + "') or (Event='" + EventName + "')]");
	}

	//Start IRBA-295: added check if Model already Exits in RulesSysInfo.xml	
	if (isModelExits(Model, objXMLEventNode))
	{		
		reloadModel(true); //IRBA-295 : reload RulesSysInfo.xml from runtime folder to replace global settings before return
		return objXMLEventNode;
	}
	//End IRBA-295
	
	XmlNode TempNode = null;

	if (GetRuleSysParm("RUNTIMEFOLDER").length() == 0)
	{
	  RuleLocation = modRuntimeCommon.GetRuntimeDirectory() + "\\";
	}
	else
	{
	  RuleLocation = "%%RUNTIMEFOLDER%%\\";
	}

	TempNode = objXMLEventNode.AppendChild(objXMLConfigurationDocument.CreateElement("ModelLocation"));
	TempNode.setTextContent(RuleLocation + "RulesModels\\" + Model);

	SaveRuleSysInfo(objXMLConfigurationDocument);
	return objXMLEventNode;

  }
  
  /**
	* check if model path is already added to event node
	* @param modelName Model name
	* @param eventName Event name
	*/
  public boolean isModelExits(String modelName, String eventName)
  {
	  return isModelExits(modelName, eventName, null);
  }
  
  /**
	* check if model path is already added to event node
	* @param modelName Model name
	* @param eventSettingNode Xml Node
	*/
  public boolean isModelExits(String modelName, XmlNode eventSettingNode)
  {
	  return isModelExits(modelName, null, eventSettingNode);
  }
  
  /**
	* check if model path is already added to event node
	* @param modelName Model name
	* @param eventName Event name
	* @param eventSettingNode Xml Node
	*/
  private boolean isModelExits(String modelName, String eventName, XmlNode eventSettingNode)
  {
	  	String modelPath = "RulesModels\\" + modelName;
	  	//Get the Event node based on passed node or Event Name
	  	if(eventSettingNode == null)
	  	{
	  		if(eventName != null && !eventName.trim().isEmpty())
	  		{
	  			eventSettingNode = this.GetEvent(eventName);
	  		}
	  		else
	  		{
	  			return false;
	  		}	  		
	  	}
	  	
	  	//No event is found return false Model does not exists.
	  	if(eventSettingNode == null)
	  	{
	  		return false;
	  	}
	  	
	  	
		//if not added then add model location to existing event.
		boolean isModelfound = false;
		XmlNodeList objModelList = eventSettingNode.SelectNodes("ModelLocation");
		for (short i = 0; i < objModelList.size(); i++)
		{
		  String modelLocation = objModelList.item(i).getInnerText();
			
		  if (modelLocation.toLowerCase().contains(modelPath.toLowerCase()))
		  {
			isModelfound = true;
			break;
		  }
		}
		
		return isModelfound;
  }
  
  //****************************************************************************************
  // FUNCTION: SetRuleSysParm                                                              *
  //                                                                                       *
  // This function accepts a unique node name searches the xml system document and returns *
  // the value of the node to the calling routing.                                         *
  //                                                                                       *
  //****************************************************************************************
  public final boolean SetRuleSysParm(String strSysParm, String strValue)
  {
	boolean tempSetRuleSysParm = false;
	XmlNode objXMLParmNode = null;
	XmlDocument objXMLConfigurationDocument = new XmlDocument();
	tempSetRuleSysParm = true;

	try
	{
	  // Load rulesSysInfo into a dom object
	  objXMLConfigurationDocument = LoadModel();

	  // Get the first occurrence of the node
	  objXMLParmNode = objXMLConfigurationDocument.SelectSingleNode("//" + strSysParm);

	  // Update the node value
	  objXMLParmNode.setTextContent(strValue);

	  String strSettingPath = modRuntimeCommon.GetRuntimeDirectory();
	  strSettingPath = strSettingPath + File.separator + "RulesSysInfo.xml"; //IRBA-331 : Added system specific file paths
	  
	  
	  DpapiHelper.encryptDp(objXMLConfigurationDocument.OuterXml(), true);
	  
	  //code for saving xml document to specfied xml file
	  objXMLConfigurationDocument.Save(strSettingPath);
	}
	catch (java.lang.Exception e)
	{
	  tempSetRuleSysParm = false;
	}

	return tempSetRuleSysParm;
  }
  
  //Start IRBA-295 : modified the save rulessysinfo functionality
  public final void SaveRuleSysInfo(XmlDocument objXMLConfigDoc)
  {	
	  String envId = ENV_ID; 
	  String strSysPath = modRuntimeCommon.GetRuntimeDirectory();
	  StringBuilder appEnvSysInfoPath = new StringBuilder(strSysPath);

	  //Start IRBA-331 : Added system specific file paths and conversion 
	  if (envId != null && !envId.trim().isEmpty())
		  appEnvSysInfoPath.append(File.separator).append(envId);
		
	  String rulesSysInfoPath = appEnvSysInfoPath.append(File.separator).append("RulesSysInfo.XML").toString();
	  rulesSysInfoPath = modRuntimeCommon.getActualFilePath(rulesSysInfoPath);
	  File rulesSysInfoFile = new File(rulesSysInfoPath);
	  //End IRBA-331
	  modRuntimeCommon.backupFile(rulesSysInfoFile); //IRBA-295: Back up old RulesSysInfo.xml		
	  
	  objXMLConfigDoc.Save(rulesSysInfoPath); //save new RulesSysInfo.xml		
	  
	  reloadModel(true); //reload RulesSysInfo.xml from runtime folder
  }
  //End IRBA-295

  public final String GetRuleSysParm(String strSysParm)
  {
	String tempGetRuleSysParm = null;
	XmlNode objXMLParmNode = null;
	XmlDocument objXMLConfigurationDocument = new XmlDocument();
	boolean IsEncrypted = false; 
	// Load rulesSysInfo into a dom object
	objXMLConfigurationDocument = LoadModel();

	// Get the first occurrence of the node
	objXMLParmNode = objXMLConfigurationDocument.SelectSingleNode("//" + strSysParm);

	// If the node is not found then return an empty string otherwise return the value of the node
	if ((objXMLParmNode == null))
	{
	  tempGetRuleSysParm = "";
	}
	else
	{
			if ((objXMLParmNode.Attributes("Encrypted") != null) && (objXMLParmNode.Attributes("Encrypted").toLowerCase().equals("true")))
			{
				IsEncrypted = true;
			}
			else
			{
				IsEncrypted = false;
			}

	  if (IsEncrypted)
	  {
		tempGetRuleSysParm = DpapiHelper.decryptDp(objXMLParmNode.getTextContent(),ENTROPYKEY);
	  }
	  else
	  {
	  tempGetRuleSysParm = objXMLParmNode.getTextContent();
	  }
	  //147523 ends
	}

	objXMLParmNode = null;
	objXMLConfigurationDocument = null;

	return tempGetRuleSysParm;
  }

  public final XmlDocument LoadModel()
  {
	  return LoadModel(true);
  }
  
  //Start IRBA-331 : Update for to load model in linux system
  public final XmlDocument LoadModel(boolean Convert) 
  {
	CacheHelper cache = new CacheHelper(); 

	XmlDocument objXMLConfigDoc = new XmlDocument();
	String strSysPath = modRuntimeCommon.GetRuntimeDirectory();
	
	String ENVID = ENV_ID != null ? ENV_ID : "";
	
	
	XmlNode GlobalNode = null;
	String appEnvSysInfoPath = "";

	// Obtain the RulesSysInfo.xml document from the cache and load into the dom object
	
	if (!ENVID.isEmpty()){		
		appEnvSysInfoPath = new File(strSysPath, File.separator + ENVID + File.separator + "RulesSysInfo.xml").getAbsolutePath();
	}
	//caching the rulessysinfo for performance improvement - starts
	Object rulesSysInfoXML = cache.getItem(ENVID + "RulesSysInfo");
	if (rulesSysInfoXML != null){		
		objXMLConfigDoc.LoadXml((String)rulesSysInfoXML);
	}
	else{
		
		if (!appEnvSysInfoPath.isEmpty()){			
		  objXMLConfigDoc.Load(appEnvSysInfoPath);
		}
		else{			
		  objXMLConfigDoc.Load(new File(strSysPath,"RulesSysInfo.xml").getAbsolutePath());
		}		

		//Replace Global settings
		//added if condition
		if (Convert){
			if (objXMLConfigDoc.SelectSingleNode("RulesSystemConfiguration/GlobalSettings") != null){
				GlobalNode = objXMLConfigDoc.SelectSingleNode("RulesSystemConfiguration/GlobalSettings");
				String ConfigXml = objXMLConfigDoc.getInnerXml();
				for (int i =0 ; i < GlobalNode.getChildNodes().getLength() ; i++){
					XmlNode ChildNode = GlobalNode.getChildNodes().item(i);
					ConfigXml = ConfigXml.replace("%%" + ChildNode.getNodeName() + "%%", ChildNode.getTextContent()); 
				}
				objXMLConfigDoc.setInnerXml(ConfigXml);
			}
		}
		cache.setItem(ENVID + "RulesSysInfo", objXMLConfigDoc.OuterXml());
	}
	//caching the rulessysinfo for performance improvement - ends
	// Return the document
	return objXMLConfigDoc;
  }
  //End IRBA-331 
  
  //Start IRBA-331
  public final Object GetAllEvents()
  {
	  Object tempGetAllEvents = null;
	  XmlNodeList objXMLEventNodeList = null;
	  XmlDocument objXMLConfigurationDocument = new XmlDocument();
	  List<String> arrEvents = new ArrayList<String>();
	  short i = 0;
	  short j = 0;
	  String strEvent = null;

	  // Load the system info file
	  objXMLConfigurationDocument = LoadModel();

	  // Build a node list of all eventsetting nodes for searching
	  objXMLEventNodeList = objXMLConfigurationDocument.SelectNodes("RulesSystemConfiguration/EventSettings/@Event");
	  // Process each item in the node list and load into the array if it is not already loaded
	  for (i = 0; i < objXMLEventNodeList.getLength(); i++)
	  {	
		  strEvent = objXMLEventNodeList.item(i).getTextContent();//IRBA-259 
		  for (j = 0; j < arrEvents.size(); j++)
		  {
			  if (strEvent.equals(arrEvents.get(j)))
			  {
				  strEvent = "";
				  break;
			  }
		  }
		  if (strEvent != null && strEvent.length() > 0)
		  {
			  // If first entry is empty then load the event into the first slot
			  if ((arrEvents.size() == 0))
			  {
				  arrEvents.add(0,strEvent);//IRBA-259 
			  }
			  else
			  {
				  arrEvents.add(objXMLEventNodeList.item(i).getTextContent());
			  }
		  }
	  }

	  objXMLEventNodeList = objXMLConfigurationDocument.SelectNodes("RulesSystemConfiguration/EventSettings/Event");
	  for (i = 0; i < objXMLEventNodeList.getLength(); i++)
	  {
		  strEvent = objXMLEventNodeList.item(i).getTextContent();
	  
		  for (j = 0; j < arrEvents.size(); j++)
		  {
			  if (strEvent.equals(arrEvents.get(j)))
			  {
				  strEvent = "";
				  break;
			  }
		  }
		  if (strEvent != null && strEvent.length() > 0) 
		  {
			  // If first entry is empty then load the event into the first slot
			  if ((arrEvents.size() == 0))
			  {
				  arrEvents.add(0,objXMLEventNodeList.item(i).getTextContent());			  
			  }
			  else
			  {	
				  arrEvents.add(objXMLEventNodeList.item(i).getTextContent());
			  }
		  }
	  }

	  // Return the array
	  tempGetAllEvents = arrEvents;
	  return tempGetAllEvents;
  }
  //End IRBA-331

  public final XmlNode GetEvent(String strEventName)
  {
	  return GetEvent(strEventName, "");
  }
  
  public final XmlNode GetEvent(String strEventName, String strVersion)
  {
	XmlNode tempGetEvent = null;
	XmlNode objXMLEventNode = null;
	XmlDocument objXMLConfigurationDocument = new XmlDocument();

	// Load RulesSysInfo into a dom object
	objXMLConfigurationDocument = LoadModel();

	// Locate the node based upon Event and Version
	if (strVersion != null && strVersion.length() > 0)
	{
	  //objXMLEventNode = objXMLConfigurationDocument.SelectSingleNode("//RulesSystemConfiguration/EventSettings[(@Event='" & strEventName & "' and @Version='" & strVersion & "') or (@Event='" & strEventName & "' and @Version>'" & strVersion & "')]")          95625
	  objXMLEventNode = objXMLConfigurationDocument.SelectSingleNode("//RulesSystemConfiguration/EventSettings[(@Event='" + strEventName + "' and @Version='" + strVersion + "') or (@Event='" + strEventName + "' and @Version>'" + strVersion + "') or (Event='" + strEventName + "' and @Version='" + strVersion + "') or (Event='" + strEventName + "' and @Version>'" + strVersion + "')]");
	}

	// If previous lookup was not performed or did not find a match then lookup without the version
	if (objXMLEventNode == null)
	{
	  //objXMLEventNode = objXMLConfigurationDocument.SelectSingleNode("RulesSystemConfiguration/EventSettings[@Event='" & strEventName & "']")
	  objXMLEventNode = objXMLConfigurationDocument.SelectSingleNode("RulesSystemConfiguration/EventSettings[(@Event='" + strEventName + "') or (Event='" + strEventName + "')]");
	}

	// Return the node
	tempGetEvent = objXMLEventNode;

	objXMLEventNode = null;
	objXMLConfigurationDocument = null;

	return tempGetEvent;
  } 

  //Start IRBA-295: reload RulesSysInfo.xml from runtime folder
  public XmlDocument reloadModel(boolean convert){
	  CacheHelper cache = new CacheHelper();
	  String ENVID = ENV_ID; 
		
	  if(ENVID == null)
	  {
		  cache.Delete("RulesSysInfo");
	  }
	  else
	  {
		  cache.Delete(ENVID+"RulesSysInfo");
	  }
	  return LoadModel(convert);
  }
  //End IRBA-295
  
}