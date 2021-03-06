package com.changyu.foryou.controller;

import java.io.File;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.changyu.foryou.model.Food;
import com.changyu.foryou.model.FoodCategory;
import com.changyu.foryou.model.FoodComment;
import com.changyu.foryou.model.FoodSpecial;
import com.changyu.foryou.model.Order;
import com.changyu.foryou.model.ShortFood;
import com.changyu.foryou.service.FoodService;
import com.changyu.foryou.service.OrderService;
import com.changyu.foryou.tools.Constants;

/**
 * 食品控制类
 * @author 殿下
 *2014/12/16
 */
@Controller
@RequestMapping("/service")
public class FoodController {
	private FoodService foodService;
	private OrderService orderService;

	protected static final Logger LOG = LoggerFactory.getLogger(FoodController.class);

	@Autowired
	public void setOrderService(OrderService orderService) {
		this.orderService = orderService;
	}

	@Autowired
	public void setFoodService(FoodService foodService) {
		this.foodService = foodService;
	}


	/**
	 * 获取食品的分类,给手机移动端(一级分类)
	 * @param campusId 校区id
	 * @return
	 */
	@RequestMapping("/getCategory")
	public @ResponseBody Map<String, Object> getFoodFirstCategory(@RequestParam Integer campusId,Integer page,Integer limit){
		Map<String, Object> map=new HashMap<String, Object>();
		try {
			Map<String,Object> paramMap=new HashMap<String,Object>();
			paramMap.put("campusId", campusId);
			if(limit!=null&&page!=null){
				paramMap.put("limit",limit);
				paramMap.put("offset",limit*(page-1));
			}

			List<FoodCategory> foodCategories=foodService.getFirstCategory(paramMap);
			if(foodCategories!=null){	
				map.put(Constants.STATUS, Constants.SUCCESS);
				map.put(Constants.MESSAGE, "获取一级分类成功");
				map.put("foodCategory", foodCategories);
			}else{
				map.put(Constants.STATUS, Constants.FAILURE);
				map.put(Constants.MESSAGE, "还没有一级分类哦");
			}
		} catch (Exception e) {
			e.getStackTrace();
			map.put(Constants.STATUS, Constants.FAILURE);
			map.put(Constants.MESSAGE, "获取一级分类失败");
		}
		return map;
	}

	/**
	 * 返回食品模糊查询
	 * @param categoryId 参数可选
	 * @param foodTag 参数可选
	 * @param sortId 排序根据 0 综合排序，1，销量排序，2价格排序
	 * @param page  显示第几页的数据
	 * @return
	 */
	@RequestMapping("/selectFoods")
	public @ResponseBody Map<String, Object>selectFoods(@RequestParam Integer campusId,String categoryId,String foodTag,Integer page,Integer limit,Integer sortId){
		Map<String, Object> map=new HashMap<String, Object>();
		try{
			List<String> foodFlags=new ArrayList<String>();
			List<ShortFood> foods=new ArrayList<ShortFood>();
			Map<String,Object> paramMap=new HashMap<>();
			paramMap.put("campusId",campusId);

			if(categoryId!=null&&categoryId.trim().equals("")){
				categoryId=null;
			}

			if(page!=null&&limit!=null){
				paramMap.put("offset",(page-1)*limit);
				paramMap.put("limit",limit);
			}

			if(sortId==null){
				sortId=0;
			}
			paramMap.put("categoryId",categoryId);
			paramMap.put("sortId",sortId);
			if(foodTag!=null&&foodTag.trim().equals("")||foodTag==null){
				foodTag=null;     //foodTag为空	
				paramMap.put("foodTag", foodTag);	
				foods= foodService.selectFoods(paramMap);
			}
			else{

				foodTag=foodTag.replace(","," ").replace(".", " ").replace(">", " ").replace("'", " ").trim();
				String[] Flags=foodTag.split(" ");
				for (int i = 0; i < Flags.length; i++) {
					if(!Flags[i].equals("")){
						foodFlags.add(Flags[i]);
					}
				}

				if(foodFlags.size()==1){
					paramMap.put("foodTag", foodFlags.get(0));
					foods= foodService.selectFoods(paramMap);
				}else{
					paramMap.put("oneFlag",foodFlags.get(0));
					paramMap.put("twoFlag",foodFlags.get(1));
					foods= foodService.selectFoodsByTwoTags(paramMap);
				}
			}

			if(foods.size()!=0){
				map.put(Constants.STATUS, Constants.SUCCESS);
				map.put(Constants.MESSAGE, "获取食品成功");
				map.put("foods", foods);
				System.out.println(JSON.toJSONString(foods));
			}else{
				map.put(Constants.STATUS, Constants.SUCCESS);
				map.put(Constants.MESSAGE, "没有其他零食喽，亲");
				map.put("foods", foods);
			}
		}catch(Exception e){
			e.getStackTrace();
			map.put(Constants.STATUS, Constants.FAILURE);
			map.put(Constants.MESSAGE, "查询零食失败");
		}
		return map;
	}

	/**
	 * 根据食品id和校区获取某个零食
	 * @param foodId
	 * @param campusId
	 * @return
	 */
	@RequestMapping("/getFoodById")
	public @ResponseBody Map<String, Object>selectFoods(@RequestParam Long foodId,@RequestParam Integer campusId){
		Map<String, Object> map=new HashMap<String, Object>();
		try{
			DecimalFormat df = new DecimalFormat("#.0");

			Map<String,Object> paramMap=new HashMap<>();
			paramMap.put("foodId", foodId);
			paramMap.put("campusId",campusId);

			Food food= foodService.selectFoodByPrimaryKey(paramMap);
			//List<FoodSpecial> foodSpecials=foodService.getFoodSpecial(paramMap);

			if(food!=null){
				Float gradeFloat=foodService.getAvageGrade(paramMap);
				if(gradeFloat==null){
					food.setGrade(0f);
				}else{
					food.setGrade(Float.parseFloat(df.format(gradeFloat)));
				}

				food.setCommentNumber(foodService.getCommentCountsById(paramMap));
				//food.setFoodSpecial(foodSpecials);
				map.put(Constants.STATUS, Constants.SUCCESS);
				map.put(Constants.MESSAGE, "获取食品成功");
				map.put("food", food);
			}else{
				map.put(Constants.STATUS, Constants.FAILURE);
				map.put(Constants.MESSAGE, "没有该零食");
			}
		}catch(Exception e){
			e.getStackTrace();
			map.put(Constants.STATUS, Constants.FAILURE);
			map.put(Constants.MESSAGE, "获取零食失败");
		}
		return map;
	}



	/**
	 * 删除食品
	 * @param foodId,campusId
	 * @return
	 */
	@RequestMapping("deleteFood")
	public @ResponseBody Map<String, Object> deleteFood(@RequestParam String foodId,@RequestParam Integer campusId){
		Map<String, Object> map=new HashMap<String, Object>();

		try {
			Map<String,Object> paramMap =new HashMap<>();
			paramMap.put("campusId",campusId);

			int status=0;
			String[] foodIdString=foodId.split(",");
			//一次删除多个零食
			for(String foodsString:foodIdString){
				paramMap.put("foodId",Long.valueOf(foodsString));
				status=foodService.deleteFoodByPrimaryKey(paramMap);
				if(status==0||status==-1){
					break;
				}
			}

			if(status!=-1){
				if(status!=0){
					map.put(Constants.STATUS, Constants.SUCCESS);
					map.put(Constants.MESSAGE, "删除零食成功！");
				}else{
					map.put(Constants.STATUS, Constants.FAILURE);
					map.put(Constants.MESSAGE, "有不存在零食，无法删除！");		
				}
			}else {
				map.put(Constants.STATUS, Constants.FAILURE);
				map.put(Constants.MESSAGE, "删除零食失败！");		
			}

		} catch (Exception e) {
			e.printStackTrace();
			map.put(Constants.STATUS, Constants.FAILURE);
			map.put(Constants.MESSAGE, "删除零食失败！");		
		}

		return map;
	}

	/**
	 * 获取某一食品的评论
	 * @param foodId
	 * @param page ，显示第几页 
	 * @param limit,当页显示数目
	 * @return
	 */
	@RequestMapping("/getCommentsByFoodId")
	public @ResponseBody Map<String, Object> getCommentsByFoodId(@RequestParam Integer campusId,@RequestParam Long foodId,Integer limit,Integer page){
		Map<String, Object> map=new HashMap<String, Object>();

		try {
			Map<String,Object> paramMap=new HashMap<>();
			paramMap.put("foodId", foodId);
			paramMap.put("campusId",campusId);

			if(page!=null&limit!=null){
				paramMap.put("offset",(page-1)*limit);
				paramMap.put("limit",limit);
			}

			List<FoodComment>foodComments=foodService.getCommentInfoById(paramMap);
			JSONArray jsonArray=JSON.parseArray(JSON.toJSONStringWithDateFormat(foodComments, "yyyy-MM-dd"));
			if(foodComments.size()!=0){
				map.put("foodComments",jsonArray);
				map.put(Constants.STATUS, Constants.SUCCESS);
				map.put(Constants.MESSAGE, "获取评论成功！");	
			}else{
				map.put(Constants.STATUS, Constants.SUCCESS);
				map.put(Constants.MESSAGE, "暂时还没有评论哦,亲！");	
			}
		} catch (Exception e) {
			e.printStackTrace();
			map.put(Constants.STATUS, Constants.FAILURE);
			map.put(Constants.MESSAGE, "获取评论失败！");	
		}
		return map;
	}

	/**
	 * 获取所有的评论
	 * @param campusId
	 * @param limit
	 * @param offset
	 * @param sort
	 * @param order
	 * @param search
	 * @return
	 */
	@RequestMapping("/getAllComments")
	public @ResponseBody Map<String, Object> getAllComments(@RequestParam Integer campusId,Integer limit,Integer offset,String sort,String order,String search){
		Map<String, Object> map = new HashMap<String, Object>();

		Map<String,Object> paramMap=new HashMap<>();
		if(sort!=null&&sort.equals("foodId")){
			sort="food_id";
		}
		paramMap.put("limit", limit);
		paramMap.put("offset",offset);
		paramMap.put("sort", sort);
		paramMap.put("order",order);
		paramMap.put("search",search);
		paramMap.put("campusId",campusId);

		JSONArray  json=JSONArray.parseArray(JSON.toJSONStringWithDateFormat(foodService.getAllComments(paramMap), "yyyy-MM-dd"));
		map.put("total", foodService.getFoodCommentCount(paramMap));
		map.put("rows", json);
		return map;
	}
	/**
	 * 获取所有的食品
	 * @return
	 */
	@RequestMapping("/getAllFoods")
	public @ResponseBody List<Food> getAllFoods(@RequestParam Integer campusId){
		List<Food> foods =new ArrayList<Food>();

		try {
			Map<String,Object> paramMap=new HashMap<String,Object>();
			paramMap.put("campusId",campusId);
			foods=foodService.getAllFoods(paramMap);	
		} catch (Exception e) {
			e.getStackTrace();
		}

		return foods;
	}

	/**
	 * 获取食品 的一级分类
	 * @return
	 */
	/*@RequestMapping("/getAllFoodFristCategorys")
	public @ResponseBody List<FoodCategory> getAllFoodFirstCategorys(){
		List<FoodCategory> foodCategories =new ArrayList<FoodCategory>();

		try {
			foodCategories=foodService.getAllFoodFirstCategories();	
		} catch (Exception e) {
			e.getStackTrace();
		}

		return foodCategories;
	}*/

	/**
	 * 获取食品的二级分类
	 * @return
	 */
	/*@RequestMapping("/getAllFoodSecondCategorys")
	public @ResponseBody List<FoodCategory> getAllFoodSecondCategorys(){
		List<FoodCategory> foodCategories =new ArrayList<FoodCategory>();

		try {
			foodCategories=foodService.getAllFoodSecondCategories();	
		} catch (Exception e) {
			e.getStackTrace();
		}

		return foodCategories;
	}*/

	/**
	 * 添加食品口味   弃用
	 * @param campusId
	 * @param foodId
	 * @param specialName
	 * @param campusId
	 * @param specialCount
	 * @return
	 */
	@RequestMapping("/addFoodSpecial")
	public @ResponseBody Map<String,Object> addFoodSpecial(@RequestParam Long foodId,@RequestParam Integer campusId,@RequestParam String specialName,@RequestParam Integer specialCount){
		Map<String, Object> map=new HashMap<String, Object>();
		FoodSpecial foodSpecial=null;
		int flag=0;

		try {
			Map<String,Object> paramMap=new HashMap<String,Object>();
			paramMap.put("campusId",campusId);
			paramMap.put("foodId",foodId);

			if(foodService.getSpecialCount(paramMap)>=3){
				map.put(Constants.STATUS,Constants.FAILURE);
				map.put(Constants.MESSAGE, "食品口味种类已饱和，不可再添加！");
				return map;
			}

			foodSpecial=new FoodSpecial(campusId,foodId,specialName,specialCount);
			Integer max=foodService.getSpecialMax(paramMap);

			if(max!=null){
				foodSpecial.setSpecialId(max+1);
			}else{
				foodSpecial.setSpecialId(0);
			}
			flag=foodService.addFoodSpecial(foodSpecial);

			if(flag!=0&&flag!=-1){
				map.put(Constants.STATUS,Constants.SUCCESS);
				map.put(Constants.MESSAGE, "添加食品口味成功！");
			}else{
				map.put(Constants.STATUS,Constants.FAILURE);
				map.put(Constants.MESSAGE, "添加食品口味失败！");
			}
		} catch (Exception e) {
			e.getStackTrace();
			map.put(Constants.STATUS,Constants.FAILURE);
			map.put(Constants.MESSAGE, "添加食品口味失败！");
		}

		return map;
	}

	/**
	 * 生成订单评论
	 * @param campusId
	 * @param phoneId
	 * @param grade
	 * @param comment
	 * @param foodId
	 * @return
	 */
	@RequestMapping(value="/creatOrderComment")
	public @ResponseBody Map<String,Object> createOrderComment(@RequestParam Integer campusId,@RequestParam String phoneId,@RequestParam Long orderId,@RequestParam Short grade,String comment,@RequestParam Long foodId){
		Map<String, Object> map=new HashMap<String, Object>();

		try {
			Order order=orderService.selectPersonOrder(phoneId,orderId);       //查询数据库看是否有订单存在，下了单才可以评论
			if(order==null){
				map.put(Constants.STATUS, Constants.FAILURE);
				map.put(Constants.MESSAGE, "没有评论权限！");
			}else{
				FoodComment foodComment=new FoodComment();
				foodComment.setComment(comment);
				foodComment.setFoodId(foodId);
				foodComment.setDate(Calendar.getInstance().getTime());
				foodComment.setGrade(grade);
				foodComment.setPhone(phoneId);
				foodComment.setTag((short)1);
				foodComment.setCampusId(campusId);

				Integer flag=foodService.insertFoodComment(foodComment);
				if(flag==1){
					orderService.updateOrderRemarked(phoneId,orderId);
					map.put(Constants.STATUS, Constants.SUCCESS);
					map.put(Constants.MESSAGE, "添加评论成功！");
				}else{
					map.put(Constants.STATUS, Constants.FAILURE);
					map.put(Constants.MESSAGE, "添加评论失败！");
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
			map.put(Constants.STATUS, Constants.FAILURE);
			map.put(Constants.MESSAGE, "添加评论失败！");
		}

		return map;
	}

	/**
	 * 删除食品评价
	 * @param foodId
	 * @param campusId
	 * @param date
	 * @param grade
	 * @return
	 */
	@RequestMapping(value="deleteFoodCommentById")
	public @ResponseBody Integer deleteFoodComment(@RequestParam Long foodId,@RequestParam Integer campusId,@RequestParam String date,@RequestParam Integer grade){
		try {
			Map<String,Object> paramMap=new HashMap<String,Object>();
			paramMap.put("foodId", foodId);
			paramMap.put("campusId",campusId);
			paramMap.put("date",date);
			paramMap.put("grade",grade);
			int flag=foodService.deleteFoodCommentById(paramMap);
			if (flag==-1) {
				return 0;
			}
		} catch (Exception e) {
			e.printStackTrace();
			return 0;
		}
		return 1;
	}

	/**
	 * 获取打折商品
	 * @param page,campusId
	 * @return
	 */
	@RequestMapping(value="/getFoodListDiscount")
	public @ResponseBody Map<String, Object>  getFoodListDiscount(@RequestParam Integer campusId,Integer limit,Integer page){
		Map<String, Object> map=new HashMap<String, Object>();

		try {
			Map<String ,Object> paramMap=new HashMap<>();
			paramMap.put("campusId", campusId);

			if(limit!=null&&page!=null){
				paramMap.put("offset", (page-1)*limit);
				paramMap.put("limit",limit);
			}
			List<ShortFood>foodlist=foodService.getFoodListDiscount(paramMap);
			map.put(Constants.STATUS, Constants.SUCCESS);
			map.put(Constants.MESSAGE, "获取数据成功！");
			map.put("foods", foodlist);
		} catch (Exception e) {
			e.printStackTrace();
			map.put(Constants.STATUS, Constants.FAILURE);
			map.put(Constants.MESSAGE, "获取食物列表失败！");
		}

		return map;
	}

	/**
	 * 获取销量好的食品
	 * @param page,limit,campusId
	 * @return
	 */
	@RequestMapping(value="/getFoodListWelcome")
	public @ResponseBody Map<String,Object> getFoodListWelcome(@RequestParam Integer campusId,Integer limit,Integer page){
		Map<String, Object> map=new HashMap<String, Object>();

		try {
			Map<String ,Object> paramMap=new HashMap<>();
			paramMap.put("campusId", campusId);

			if(limit!=null&&page!=null){
				paramMap.put("offset", (page-1)*limit);
				paramMap.put("limit",limit);
			}

			List<ShortFood>foodlist=foodService.getFoodListWelcome(paramMap);
			map.put(Constants.STATUS, Constants.SUCCESS);
			map.put(Constants.MESSAGE, "获取数据成功！");
			map.put("foods", foodlist);
		} catch (Exception e) {
			e.printStackTrace();
			map.put(Constants.STATUS, Constants.FAILURE);
			map.put(Constants.MESSAGE, "获取食物列表失败！");
		}

		return map;
	}

	/**
	 * 获取新品
	 * @param limit,page,campusId
	 * @return
	 */
	@RequestMapping(value="/getFoodListFresh")
	public @ResponseBody Map<String,Object> getFoodListFresh(Integer limit,@RequestParam Integer campusId,Integer page){
		Map<String, Object> map=new HashMap<String, Object>();

		try {
			Map<String ,Object> paramMap=new HashMap<>();
			paramMap.put("campusId", campusId);

			if(limit!=null&&page!=null){
				paramMap.put("offset", (page-1)*limit);
				paramMap.put("limit",limit);
			}

			List<ShortFood>foodlist=foodService.getFoodListFresh(paramMap);
			map.put(Constants.STATUS, Constants.SUCCESS);
			map.put(Constants.MESSAGE, "获取数据成功！");
			map.put("foods", foodlist);
		} catch (Exception e) {
			e.printStackTrace();
			map.put(Constants.STATUS, Constants.FAILURE);
			map.put(Constants.MESSAGE, "获取食物列表失败！");
		}

		return map;
	}

	/**
	 * 添加分类
	 * @param categoryId
	 * @param categoryName
	 * @param status 0添加，1更新
	 * @param campusId 校区id
	 * @return
	 */
	@RequestMapping(value="updateFoodCategory")
	public @ResponseBody Map<String, Object> updateFoodFristCategory(@RequestParam Integer campusId,Integer categoryId,@RequestParam String categoryName,Integer status){
		Map<String, Object> map=new HashMap<String, Object>();

		try {
			FoodCategory foodCategory=new FoodCategory();
			foodCategory.setCategoryId(categoryId);
			foodCategory.setCategory(categoryName);
			foodCategory.setTag((short)1);
			foodCategory.setParentId(0);
			foodCategory.setCampusId(campusId);

			Map<String,Object> paramMap=new HashMap<String,Object>();
			paramMap.put("categoryId",categoryId);
			paramMap.put("campusId",campusId);
			int flag=0;
			if(categoryId!=null){
				//查询是否已存在该分类
				FoodCategory foodCategory2=foodService.selectCategoryByPrimaryKey(paramMap);	
				if(foodCategory2==null){
					flag=foodService.insertCategorySelective(foodCategory);
				}else{
					//id已存在
					if(status==0){
						map.put(Constants.STATUS, Constants.FAILURE);
						map.put(Constants.MESSAGE,"id已存在，不能添加");
						return map;
					}else{
						flag=foodService.updateCategoryByPrimaryKeySelective(foodCategory);   //更新该分类
					}
				}		
			}else{        //新增商品
				flag=foodService.insertCategorySelective(foodCategory);
			}
			if(flag==0||flag==-1){
				map.put(Constants.STATUS, Constants.FAILURE);
				map.put(Constants.MESSAGE, "提交失败");
			}else{
				map.put(Constants.STATUS, Constants.SUCCESS);
				map.put(Constants.MESSAGE, "提交成功");
			}
		} catch (Exception e) {
			e.printStackTrace();
			map.put(Constants.STATUS, Constants.FAILURE);
			map.put(Constants.MESSAGE, "提交失败");
		}

		return map;
	}

	//弃用
	/*
	@RequestMapping("/updateFoodSecondCategory")
	public String updateFoodSecondCategory(@RequestParam MultipartFile myfile, HttpServletRequest request)throws IOException{
		FoodCategory foodCategory=new FoodCategory();
		int flag=0;
		String categoryId=request.getParameter("categoryId");
		String categoryName=request.getParameter("categoryName");
		String parentId=request.getParameter("parentId");

		try {
			if(myfile.isEmpty()){  
				System.out.println("文件未上传"); 
				foodCategory.setCategoryId(Integer.valueOf(categoryId));
				foodCategory.setParentId(Integer.valueOf(parentId));
				foodCategory.setCategory(categoryName);

				flag=foodService.updateCategoryByPrimaryKeySelective(foodCategory);
				return "redirect:/pages/food_second_category.html";
			}else{ 			
				String contentType=myfile.getContentType();

				if(contentType.startsWith("image")){
					String realPath = request.getSession().getServletContext().getRealPath("/"); 

					realPath=realPath.replace("SJFood", "MickeyImage");
					realPath=realPath.concat("\\foodcategory\\");             //获取服务器图片路径

					System.out.println(realPath);
					String newFileName=new Date().getTime()+""+new Random().nextInt()+".jpg";       //重新设置图片名字
					FileUtils.copyInputStreamToFile(myfile.getInputStream(), new File(realPath, newFileName));    //将文件上传到服务器

					String imageUrl=Constants.localIp+"/foodcategory/"+newFileName;
					foodCategory.setImgUrl(imageUrl);
					foodCategory.setCategoryId(Integer.valueOf(categoryId));
					foodCategory.setParentId(Integer.valueOf(parentId));
					foodCategory.setCategory(categoryName);
					foodCategory.setTag((short)1);

					FoodCategory foodCategory2=foodService.selectCategoryByPrimaryKey(foodCategory.getCategoryId());
					if(foodCategory2==null){
						flag=foodService.insertCategorySelective(foodCategory);   //不存在即添加
					}
					else {
						flag=foodService.updateCategoryByPrimaryKeySelective(foodCategory);  //存在即更新

						//删除原来的食品分类的图片
						if(foodCategory2.getImgUrl()!=null&&foodCategory.getImgUrl()!=null){
							String[] temp=foodCategory2.getImgUrl().split("/");

							String imageName=temp[(temp.length-1)];
							String name=realPath+imageName;

							System.out.println(name);;
							File file=new File(name);
							if(file.isFile()){
								file.delete();//删除
							}
						}
					}

					if(flag!=0&&flag!=-1){
						return "redirect:/pages/food_second_category.html";
					}
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
			return "redirect:/pages/uploadError.html";
		}

		return "redirect:/pages/uploadError.html";

	}*/

	/**
	 * 
	 * @param categoryIds
	 * @return
	 */
	@RequestMapping("/deleteFoodCategory.do")
	public @ResponseBody Map<String, Object>deleteFoodCategory(@RequestParam Integer campusId,@RequestParam String categoryIds){
		Map<String, Object> map=new HashMap<>();

		String[] categoryidsString=categoryIds.split(",");

		try {
			Map<String,Object> paramMap=new HashMap<String,Object>();
			paramMap.put("campusId",campusId);
			int flag=0;
			
			//渐次删除分类
			for (String categoryId:categoryidsString) {
				if(categoryId!=null&&!categoryId.trim().equals("")){
					paramMap.put("categoryId",Integer.valueOf(categoryId));
					flag=foodService.deleteCategoryByPrimaryKey(paramMap);	
				}
			}

			if (flag==-1||flag==0) {
				map.put(Constants.STATUS, Constants.FAILURE);
				map.put(Constants.MESSAGE, "删除失败");
			}else{
				map.put(Constants.STATUS, Constants.SUCCESS);
				map.put(Constants.MESSAGE, "删除成功");

			}

		} catch (Exception e) {
			e.printStackTrace();	
			map.put(Constants.STATUS, Constants.FAILURE);
			map.put(Constants.MESSAGE, "删除失败");
		}
		return map;
	}

	/**
	 * 在pc端添加食品
	 * @param myfile
	 * @param request
	 * @return
	 */
	@RequestMapping(value="/updateFoods.do")
	public String updateFoods(@RequestParam MultipartFile[] myfile,HttpServletRequest request){
		try {
			Long foodId=Long.valueOf(request.getParameter("foodId"));            //获取食品id
			Float price=Float.valueOf(request.getParameter("price"));                //获取价格
			String name=request.getParameter("foodName");                            //获取食品名称
			Float discountPrice=Float.valueOf(request.getParameter("discountPrice"));   //获取折扣价
			Short status=Short.valueOf(request.getParameter("status"));           //获取食品上架下架状态
			Short isDiscount=Short.valueOf(request.getParameter("is_discount"));    //是否打折
			String foodFlag=request.getParameter("foodTag");            //食品标签
			Integer categoryId=Integer.valueOf(request.getParameter("categoyId"));  //获取分类Id
			Float primeCost=null;

			String temp1=request.getParameter("primeCost");       //获取成本价
			String temp2=request.getParameter("foodCount");       //获取食品数量
			Integer campusId=Integer.valueOf(request.getParameter("campusId"));  //获取校区
			if(temp1!=null&&!temp1.trim().equals("")){
				primeCost=Float.valueOf(request.getParameter("primeCost"));
			}
			Integer foodCount=null;
			if(temp2!=null&&!temp2.trim().equals("")){
				foodCount=Integer.valueOf(request.getParameter("foodCount"));
			}

			Short isDefault=Short.valueOf(request.getParameter("default_special"));
			String realPath = request.getSession().getServletContext().getRealPath("/"); 
			realPath=realPath.replace("SJFood", "MickeyImage");
			realPath=realPath.concat("\\food\\");
			System.out.println(realPath);               //打印出服务器路径

			List<String> imageUrl=new ArrayList<String>();
			for(MultipartFile file:myfile){
				if(file.isEmpty()){  
					System.out.println("文件未上传");  
					imageUrl.add(null);
				}else{  			
					String contentType=file.getContentType();

					if(contentType.startsWith("image")){
						String newFileName=new Date().getTime()+""+new Random().nextInt()+".jpg";
						FileUtils.copyInputStreamToFile(file.getInputStream(), new File(realPath, newFileName));  //写文件
						imageUrl.add(Constants.localIp+"/food/"+newFileName);
					}
				}  
			}
			Food food=new Food(campusId,foodId, name, price, discountPrice, imageUrl.get(0), imageUrl.get(1), status,foodFlag, isDiscount, categoryId, primeCost);
			
			Map<String,Object> paramMap=new HashMap<String,Object>();
			paramMap.put("campusId",campusId);
			paramMap.put("foodId", foodId);
			Food orignFood=foodService.selectFoodByPrimaryKey(paramMap);   //查看该食品是否存在
			int flag=0;
			if (orignFood==null) {
				//不存在即添加
				flag=foodService.insertFoodSelective(food);
				//只有在增加食品的时候添加默认口味才有效
				if (flag!=-1&&flag!=0) {
					if(isDefault==1&&foodCount!=null){
						//添加默认口味
						FoodSpecial foodSpecial=new FoodSpecial(campusId,foodId, "默认口味", foodCount);
						foodSpecial.setSpecialId(1);
						flag=foodService.addFoodSpecial(foodSpecial);
						if(flag!=-1&&flag!=0){
							return "redirect:/pages/food.html";
						}
					}
				}
			}else{
				//存在即更新
				flag=foodService.updateFoodByPrimaryKeySelective(food);

				//删除原食品主图片
				if(food.getImgUrl()!=null&&orignFood.getImgUrl()!=null){
					String[] temp=orignFood.getImgUrl().split("/");
					String imageName=temp[(temp.length-1)];

					String name2=realPath+imageName;

					File file=new File(name2);
					if(file.isFile()){
						file.delete();//删除
					}
				}

				//删除原食品辅助图片
				if(food.getInfo()!=null&&orignFood.getInfo()!=null){
					String[] temp=orignFood.getInfo().split("/");
					String imageName=temp[(temp.length-1)];

					String name2=realPath+imageName;

					File file=new File(name2);
					if(file.isFile()){
						file.delete();//删除
					}
				}

				if(flag!=-1&&flag!=0){
					return "redirect:/pages/food.html";
				}
			}

		} catch (Exception e) {
			e.printStackTrace();
			return "redirect:/pages/uploadError.html";
		}
		return "redirect:/pages/uploadError.html";
	}

	/**
	 * 添加新食品   弃用
	 * @param foodId,request
	 * @param name,request
	 * @param price,request
	 * @param discountPrice
	 * @param grade
	 * @param imgUrl
	 * @param info
	 * @param status
	 * @param foodCount
	 * @param foodFlag
	 * @param tag
	 * @param isDiscount
	 * @param categoryId
	 * @param primeCost
	 * @return
	 */
	/*@RequestMapping("/addNewFood")
	public @ResponseBody Map<String, Object> addNewFood(@RequestParam Long foodId,@RequestParam String name, @RequestParam String price,String discountPrice,String grade, String imgUrl, String info, String status, 
			String foodCount, String foodFlag, String tag, String isDiscount, String categoryId, String primeCost){
		Map<String, Object> map=new HashMap<String, Object>();

		try{
			Food food=new Food(foodId,name,price,discountPrice,grade,
					imgUrl,info,status,foodCount,foodFlag,tag,isDiscount,categoryId,
					primeCost);

			food.setSaleNumber(Long.valueOf("0"));
			if((foodService.selectFoodByPrimaryKey(foodId))!=null){
				map.put(Constants.STATUS, Constants.FAILURE);
				map.put(Constants.MESSAGE, "食品id重复,请更换食品id并做好记录");
				return map;
			}else{
				if(foodService.insertFoodSelective(food)!=-1){
					map.put(Constants.STATUS, Constants.SUCCESS);
					map.put(Constants.MESSAGE, "添加食品成功");
				}else{
					map.put(Constants.STATUS, Constants.FAILURE);
					map.put(Constants.MESSAGE, "添加食品失败");
				}
			}
		}catch(Exception exception){
			exception.getStackTrace();
			map.put(Constants.STATUS, Constants.FAILURE);
			map.put(Constants.MESSAGE, "添加食品失败");
		}
		return map;
	}*/

	/*@RequestMapping(value="/getSpecial",method=RequestMethod.POST)
	public @ResponseBody Map<String, Object> getFoodSpecialById(@RequestParam Long foodId){
		Map<String, Object> map=new HashMap<String,Object>();

		try {
			List<FoodSpecial> foodSpecials=foodService.getFoodSpecial(foodId);
			map.put(Constants.STATUS, Constants.SUCCESS);
			map.put(Constants.MESSAGE, "获取成功!");
			map.put("specialList", foodSpecials);
		} catch (Exception e) {
			e.printStackTrace();
			map.put(Constants.STATUS, Constants.FAILURE);
			map.put(Constants.MESSAGE, "获取失败");
		}

		return map;
	}*/

	/**
	 * 弃用，不再有口味
	 * @param campusId
	 * @param foodId
	 * @param speicalId1
	 * @param speicalId2
	 * @param specialId3
	 * @param specialName1
	 * @param specialName2
	 * @param specialName3
	 * @param specialCount1
	 * @param specialCount2
	 * @param specialCount3
	 * @param isDelete1
	 * @param isDelete2
	 * @param isDelete3
	 * @return
	 */
	@RequestMapping(value="/updateSpecialById")
	public @ResponseBody Map<String, Object> updateSpecialById(@RequestParam Integer campusId,@RequestParam Long foodId,String speicalId1,String speicalId2,String
			specialId3,String specialName1,String specialName2,String specialName3,Integer specialCount1,Integer specialCount2,Integer specialCount3,
			boolean isDelete1,boolean isDelete2,boolean isDelete3){
		Map<String, Object> map=new HashMap<String,Object>();

		//一次性更新多个食品口味的数量或者名字
		String[] specialIds={speicalId1,speicalId2,specialId3};
		String[] specialNames={specialName1,specialName2,specialName3};
		Integer[] specialCounts={specialCount1,specialCount2,specialCount3};
		boolean[] isDeletes={isDelete1,isDelete2,isDelete3};

		try {
			for(int i=0;i<3;i++){
				if(specialIds[i]!=null&&!specialIds[i].equals("")){
					FoodSpecial foodSpecial=new FoodSpecial(campusId,foodId, specialNames[i], specialCounts[i]);
					foodSpecial.setSpecialId(Integer.valueOf(specialIds[i]));
					if(isDeletes[i]){
						//删除口味
						foodService.deleteFoodSpecial(foodSpecial);
					}else{
						//更新口味数量
						foodService.updateFoodSpecial(foodSpecial);
					}
				}
			}
			map.put(Constants.STATUS, Constants.SUCCESS);
			map.put(Constants.MESSAGE, "更新口味成功");
		} catch (Exception e) {                                                                                                                                                                                                                       
			e.printStackTrace();
			map.put(Constants.STATUS, Constants.FAILURE);
			map.put(Constants.MESSAGE, "更新口味失败");
		}

		return map;
	}
}
