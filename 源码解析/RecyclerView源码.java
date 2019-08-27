#RecyclerView 源码
#RecyclerView.LayoutManager 负责视图的布局(线性，Grid，瀑布流)
#ItemDecoration控制着间隔，分割线之类的
#ItemAnimator控制着动画

#Adapter 负责RecyclerView的缓存之类的
#每一个item就是一个ViewHolder 
/**
通过桥接模式，使RecyclerView 将布局方式独立成LayoutManager，实现对布局的定制化。
通过组合模式，使RecycleView通过dispatchLayout对Item View进行布局绘制的。
通过适配器模式，ViewHolder将RecycleView与ItemView联系起来，
使得RecycleView方便操作ItemView。
通过观察者模式，给ViewHolder注册观察者，
当调用notifyDataSetChanged时，就能重新绘制。

**/


