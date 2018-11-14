package org.exoplatform.datacollector;

import java.util.Locale;
import java.util.ResourceBundle;

import org.exoplatform.commons.utils.PageList;
import org.exoplatform.services.resources.Query;
import org.exoplatform.services.resources.ResourceBundleData;
import org.exoplatform.services.resources.ResourceBundleService;

public class ResourceBundleServiceMock implements ResourceBundleService {

  @Override
  public ResourceBundle getResourceBundle(String name, Locale locale) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public ResourceBundle getResourceBundle(String name, Locale locale, ClassLoader cl) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public ResourceBundle getResourceBundle(String[] name, Locale locale) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public ResourceBundle getResourceBundle(String[] name, Locale locale, ClassLoader cl) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public ResourceBundleData getResourceBundleData(String id) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public ResourceBundleData removeResourceBundleData(String id) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public void saveResourceBundle(ResourceBundleData data) {
    // TODO Auto-generated method stub
    
  }

  @Override
  public PageList<ResourceBundleData> findResourceDescriptions(Query q) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public ResourceBundleData createResourceBundleDataInstance() {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public String[] getSharedResourceBundleNames() {
    // TODO Auto-generated method stub
    return null;
  }

}
