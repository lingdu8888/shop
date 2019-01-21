package com.jiagouedu.web.action.manage.catalog;import com.google.common.collect.Lists;import com.jiagouedu.core.front.SystemManager;import com.jiagouedu.core.oscache.FrontCache;import com.jiagouedu.core.util.PinYinUtil;import com.jiagouedu.services.manage.catalog.CatalogService;import com.jiagouedu.services.manage.catalog.bean.Catalog;import com.jiagouedu.web.action.BaseController;import net.sf.json.JSONArray;import org.apache.commons.lang.StringUtils;import org.slf4j.Logger;import org.slf4j.LoggerFactory;import org.springframework.beans.factory.annotation.Autowired;import org.springframework.stereotype.Controller;import org.springframework.ui.ModelMap;import org.springframework.web.bind.annotation.ModelAttribute;import org.springframework.web.bind.annotation.RequestMapping;import org.springframework.web.bind.annotation.RequestMethod;import org.springframework.web.bind.annotation.ResponseBody;import org.springframework.web.servlet.mvc.support.RedirectAttributes;import javax.servlet.http.HttpServletRequest;import java.io.IOException;import java.util.List;/** * 商品分类,可以无限极分类 *  * @author wukong 图灵学院 QQ:245553999 * @author wukong 图灵学院 QQ:245553999 *  */@Controller@RequestMapping("/manage/catalog/")public class CatalogAction extends BaseController<Catalog> {	private static final Logger logger = LoggerFactory			.getLogger(CatalogAction.class);	private static final long serialVersionUID = 1L;    @Autowired	private CatalogService catalogService;    @Autowired	private FrontCache frontCache;    @Autowired    private SystemManager systemManager;    private static final String page_toList = "/manage/catalog/catalogList";//    <result name="selectAllList" type="redirect">catalog!selectList.action?e.type=${e.type}</result>    private static final String page_toAdd = "/manage/catalog/catalogEdit";    private static final String page_toEdit = "/manage/catalog/catalogEdit";    private CatalogAction() {        super.page_toList = page_toList;        super.page_toAdd = page_toAdd;        super.page_toEdit = page_toEdit;    }	public FrontCache getFrontCache() {		return frontCache;	}	public void setFrontCache(FrontCache frontCache) {		this.frontCache = frontCache;	}    @Override	public CatalogService getService() {		return catalogService;	}	/**	 * 公共的分页方法	 *	 * @return	 * @throws Exception	 */	@RequestMapping("selectList")	public String selectList(HttpServletRequest request, @ModelAttribute("e") Catalog e) throws Exception {		List<Catalog> root = catalogService.loadRoot(e);		List<Catalog> result = Lists.newArrayList();		for(Catalog cata : root){			appendChildren(cata, result);		}		request.setAttribute("list", result);		return page_toList;	}	private void appendChildren(Catalog catalog, List<Catalog> list) {		if(catalog == null){			return;		}		list.add(catalog);		if(catalog.getChildren() != null && catalog.getChildren().size() > 0) {			for (Catalog cata : catalog.getChildren()) {				appendChildren(cata, list);			}		}	}	public void setCatalogService(CatalogService catalogService) {		this.catalogService = catalogService;	}    @Override	public void insertAfter(Catalog e) {	}	/**	 * 递归查询数据库获取商品目录	 * 返回tree的数据结构 从PID=0开始加载菜单资源 获取指定节点的全部子菜单（包括当前菜单节点）	 * 	 * @return	 * @throws Exception	 */    @RequestMapping("getRoot")    @ResponseBody	public String getRoot(Catalog e) throws Exception {		List<Catalog> root = catalogService.loadRoot(e);		JSONArray json = JSONArray.fromObject(root);		logger.debug("catalog json : " + json.toString());		String jsonStr = json.toString();		return jsonStr;	}	/**	 * 数据来自缓存	 * 返回适合easyui.treegrid的JSON的数据结构 从PID=0开始加载菜单资源 获取指定节点的全部子菜单（包括当前菜单节点）	 * 	 * @return	 * @throws Exception	 */    @RequestMapping("getRootWithTreegrid")    @ResponseBody	public String getRootWithTreegrid(Catalog e) throws Exception {		logger.error(">>>selectList type = "+e.getType());				List<com.jiagouedu.services.manage.catalog.bean.FrontCatalog> root = null;		if(e.getType().equals("p")){			//直接使用缓存数据            String productCatalogJsonStr = systemManager.getProductCatalogJsonStr();            if(productCatalogJsonStr !=null){                logger.debug("product catalog json str from cache : {}", productCatalogJsonStr );				return productCatalogJsonStr;			}						root = systemManager.getCatalogs();						JSONArray json = JSONArray.fromObject(root);			productCatalogJsonStr = json.toString();            systemManager.setProductCatalogJsonStr(productCatalogJsonStr);            logger.debug("product catalog json str : {}", productCatalogJsonStr);            return productCatalogJsonStr;					}else if(e.getType().equals("a")){			//直接使用缓存数据            String articleCatalogJsonStr = systemManager.getArticleCatalogJsonStr();            if(articleCatalogJsonStr !=null){                logger.debug("article catalog json str from cache : {}", articleCatalogJsonStr);                return articleCatalogJsonStr;			}						root = systemManager.getCatalogsArticle();						JSONArray json = JSONArray.fromObject(root);			articleCatalogJsonStr = json.toString();            systemManager.setArticleCatalogJsonStr(articleCatalogJsonStr);            logger.debug("article catalog json str : {}", articleCatalogJsonStr);            return articleCatalogJsonStr;		}else{			throw new IllegalAccessError("参数异常。");		}	}	/**	 * 根据ID删除指定的目录,如果该类目下面有子类目,则会一并删除;如果该类目下面有商品,则会一并删除	 * 	 * @return	 * @throws Exception	 */    @RequestMapping(value = "deleteByID", method = RequestMethod.POST)    @ResponseBody	public String deleteByID(String id) throws Exception {		if (StringUtils.isBlank(id)) {			throw new NullPointerException("参数不正确！");		}				boolean isSuccess = catalogService.deleteByID(id);		logger.info("delete resule : {}", isSuccess);		reset();		return String.valueOf(isSuccess);	}		/**	 * 添加/修改/删除 某一个类别后，需要重新加载缓存数据。并且清除JSON字符串缓存，以便重新生成新的。	 * @throws Exception	 */	private void reset() throws Exception {        systemManager.setProductCatalogJsonStr(null);        systemManager.setArticleCatalogJsonStr(null);		frontCache.loadCatalogs(true);//同步更新缓存	}	/**	 * 不支持批量删除	 */    @Override	public String deletes(HttpServletRequest request, String[] ids, Catalog e, RedirectAttributes flushAttrs) throws Exception {		throw new NullPointerException();	}	@Override    @RequestMapping("toAdd")	public String toAdd(Catalog e, ModelMap model) throws Exception {		String type = e.getType();		logger.error("CatalogAction.toAdd.type="+e.getType());		e.clear();		e.setType(type);        model.addAttribute("e", e);        if("p".equals(type)) {            model.addAttribute("catalogs", systemManager.getCatalogs());        } else {            model.addAttribute("catalogs", systemManager.getCatalogsArticle());        }		return page_toAdd;	}	@Override    @RequestMapping("toEdit")	public String toEdit(Catalog e, ModelMap model) throws Exception {		if(StringUtils.isBlank(e.getId())){			throw new NullPointerException("非法请求！");		}		String _id = e.getId();		e.clear();		e.setId(_id);		e = getService().selectOne(e);        model.addAttribute("e", e);		if(e==null){			throw new NullPointerException("非法请求！");		}        if("p".equals(e.getType())) {            model.addAttribute("catalogs", systemManager.getCatalogs());        } else {            model.addAttribute("catalogs", systemManager.getCatalogsArticle());        }		return page_toEdit;	}	/**	 * 返回到查询页面	 *///	public String back() throws Exception {//		return selectList();//	}	@Override    @RequestMapping(value = "insert", method = RequestMethod.POST)	public String insert(HttpServletRequest request, Catalog e, RedirectAttributes flushAttrs) throws Exception {		if(StringUtils.isBlank(e.getPid())){			e.setPid("0");		}		String type = e.getType();		logger.error("type = "+type);		try {			getService().insert(e);			e.clear();		} catch (Exception ex) {			ex.printStackTrace();			throw ex;		}				reset();		addMessage(flushAttrs, "新增成功！");//		e.setType(type);		return "redirect:selectList?type="+type;	}    @Override    @RequestMapping(value = "update", method = RequestMethod.POST)	public String update(HttpServletRequest request, Catalog e, RedirectAttributes flushAttrs) throws Exception {		String type = e.getType();		logger.error("type = "+type);		try {			getService().update(e);			e.clear();		} catch (Exception ex) {			ex.printStackTrace();			throw ex;		}				reset();		addMessage(flushAttrs, "更新成功！");//		getE().setType(type);		return "redirect:selectList?type="+type;	}		/**	 * 唯一性检查	 * @return	 * @throws IOException 	 */    @RequestMapping(value = "uniqueCode", method = RequestMethod.POST)    @ResponseBody	public String uniqueCode(Catalog e) throws IOException{		logger.error("unique code = " + e.getCode());		if(StringUtils.isNotBlank(e.getCode())){			Catalog catalog = new Catalog();			catalog.setCode(e.getCode());			catalog = catalogService.selectOne(catalog);			if(catalog==null){				//数据库中部存在此编码				return "{\"ok\":\"编码可以使用!\"}";			}else{				if(StringUtils.isNotBlank(e.getId()) && StringUtils.trimToEmpty(e.getId()).equals(catalog.getId())) {					//update操作，又是根据自己的编码来查询的，所以当然可以使用啦					return "{\"ok\":\"编码可以使用!\"}";				} else {					//当前为insert操作，但是编码已经存在，则只可能是别的记录的编码					return "{\"error\":\"编码已经存在!\"}";				}			}		}else{			return "{\"error\":\"编码不能为空!\"}";		}//		return null;	}		/**	 * 根据类别名称自动获取拼音-ajax	 * @return	 * @throws IOException 	 */    @RequestMapping(value = "autoCode", method = RequestMethod.POST)    @ResponseBody	public String autoCode(Catalog e) throws IOException{		if(StringUtils.isBlank(e.getName())){			return null;		}				final String pinyin = PinYinUtil.getPingYin(e.getName());		logger.error("pinyin="+pinyin);        String _pinyin = pinyin;		for(int i = 1; true; i++){			Catalog c = new Catalog();			c.setCode(_pinyin);			c = catalogService.selectOne(c);			if(c==null){				return _pinyin;//				break;			}else{                _pinyin = pinyin + i;			}		}//		return null;	}}