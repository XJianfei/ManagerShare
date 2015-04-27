package com.peter.parttime.managershare;

import android.app.Activity;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.Html;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.Window;
import android.widget.TextView;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;


public class WebArticleActivity extends Activity {

    public static final String EXTRA_URL = "extra_url";

    private TextView mArticalContentTextView = null;
    //private String mArticalContent = "";
    private String mArticalContent = "<p>大半个月前，我去参加某高大上互联网公司股权众筹的发布会，在台上演讲的高管说，他们不仅给创业者真金白银，还给创业者提供导师和培训。在一闪而过的PPT上，我看到了一堆创业导师的名单，他们是网红、连续创业失败者、自媒体人等等，当然，免不了的还有几个投资人。<br/><br/>几天之后，我写了篇O2O稿子，有人加我微信。此人自我介绍是著名培训师，帮助传统企业转型互联网，“时髦一点，也可以说是O2O、互联网＋”。看来他认真学习了李克强的两会讲话啊。<br/><br/>此人唯一的创业经历，就是淘宝开店，据说店铺很小但是不美，经济困窘生活无着，转作电商培训师。“听我讲课的人特别多”，瞎扯几句后，这位“著名”培训师说，“你也可以当培训师。”<br/><br/>“我都没做过，我就瞎扯几句过过嘴瘾啊。。。。。”<br/><br/>“别心虚，自信点，没做过不代表你不懂，你可以通过帮助别人来成就自己”。嗯，他特别强调了一句自己的专业素养，“我是成功学大师陈安之的学生”。<br/><br/>恰好很多年前，陈安之的助理给我所在的小杂志打电话求采访，我怀着对成功学大师的各种好奇应承了。这位女助理跟我说，“x记者，陈安之是伟大的思想家和演讲家，您的问题太犀利了不好，对老师应该是心怀仰视和尊重。”她花了二十分钟，努力纠正我面访“大师”的正确姿势。<br/><br/>过了一会，白西装白西裤的陈安之就跳着舞步扭了过来。我问他：“你的助理说你是思想家，你觉得你是孔子老子那种么？”<br/><br/>他迟疑了那么一两秒钟，然后很确信的说：“嗯，我觉得你这个比喻很恰当。”然后他抬起脚，指了指自己的阿玛尼（可能我的记忆有偏差）皮鞋说，“我只出精品演讲和培训，就像阿玛尼一样”。<br/><br/>我还有个跑电商很多年的记者同行，很多人找她讲课，都被拒绝了。因为她觉得自己没有操盘手的经验，怕虚头巴脑的误人子弟，“别想从培训课找到答案，那是不可能的。”<br/><br/>有次，她终于耐不住某咨询机构电商培训师的多次热情邀约去给捧捧场，讲课的主题是马云的成功秘笈之类，“你们知道吗，阿里巴巴现在要转型做B2B了，B2B才是未来。。。。”<br/><br/>这位同行实在忍受不了这种可耻的常识性错误，屁股像针扎一样，立刻就出了门，远远的听到，这位培训大师自信满满的说：“下次我给你们传授下小米雷军的成功法宝”。<br/><br/>我实在不相信这位电商大师愚蠢到如此天真，故意引导到B2B，也许是为了在课堂下，推广营销他的新产品新课程吧。<br/><br/>这是一个人人都能创业的年代，年轻人裹挟于创业创新的洪流之中，仿佛迈过创业的门槛，就跃上了龙门，人人都想成为下一个马云马化腾李彦宏，人人都想成就下一个阿里百度和腾讯。创业仿佛成为了某种政治正确的主流路径，连克强总理都在鼓励大家创业创新。<br/><br/>但是，中国的互联网行业基本上只有创业没有创新，抄袭和山寨盛行。但是，因为多数创业者缺乏创业的自信和能力，不愿意孤独的创新，而太平洋那边的成功案例，仿佛是武学界的九阴真经，只要拿到手就能练就独步武林的绝世武功。</p><p>模仿山寨者众，也导致每个创业领域都成为了血流遍地、尸身遍野的红海，人人自危，没有安全感，所以才需要所谓的“导师”们加持吧。传统企业触网，自我革命，免不了也有类似战战兢兢的恐惧。<br/><br/><strong>可惜创业者很多，“李开复”不够，所以连一大批机场成功学大师的徒子徒孙们也能成功上位。</strong><br/><br/>后来，我想明白了，创业导师本身可能也是一种创业。创业者和创业导师的关系，可以类比为铁锹和金矿。1849年，美国加州发现金矿后，淘金者纷至沓来。金子怎么可能一挖即着?想要淘到真金，需要不懈努力。于是，相应的服务行业应运而生——卖铁锹卖牛仔裤。</p><p>结果卖铁锹的致富了，挖金子的未必能发财。不过，铁锹牛仔裤是拿在手里穿在腿上沉甸甸可触摸的实物，而创业导师的洋洋之语，到底是毒药还是蜜糖，各位创业者，你也许要用创业项目的生死与否去证明了。<br/><br/>那天从某司发布会回来，我默默的发了一条朋友圈：<br/><br/>刚看了创业导师的名单，赫然发现，刚开始创业的都去当创业导师了，可能创业导师也是一门生意，创业导师要和创业者一块成长同甘共苦吧。<br/><br/>免责声明：我很尊重李开复老师，还有那些在某个行业认真钻研、真的能讲出各种干货、总结出方法论的创业导师们，你们也是棒棒哒，向你们致敬！但是你们实在太稀缺了，所以这个行业泥沙俱下，忽悠者众！<br/><br/><strong>（作者介绍：陈纪英，微信公众号：财经故事会，caijinggushi）</strong></p>\n";
    private String mArticalTitle = "";
    private String mPath = "";
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.web_article);
        mArticalContentTextView = (TextView) findViewById(R.id.content);
        mPath = getIntent().getStringExtra(EXTRA_URL);
        ManagerShareActivity.info("show: " + mPath);
        if (mPath == null) {
            mArticalContentTextView.setText(R.string.invalid_url);
            return;
        }

        new Thread(mGetArticalRunnable).start();
    }

    private Document getWebDocument(String url) throws IOException {
        ManagerShareActivity.dbg("http:" + url);
        Document doc = Jsoup.connect(url).get();
        return doc;
    }

    private static final int MSG_GET_WEB_CONTENT_DONE = 0;
    private static final int MSG_GET_WEB_CONTENT_FAILED = 1;
    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MSG_GET_WEB_CONTENT_DONE:
                    //mArticalContentTextView.setMovementMethod(ScrollingMovementMethod.getInstance());
                    mArticalContentTextView.setText(
                            Html.fromHtml(
                                    "<html><head><title>" +
                                    "<strong><font color=\"#00000000\">" +
                                    mArticalTitle + "</strong>" +
                                    "</title></head><br/><br/>" +
                                    "<body>" +
                                    "<font color=\"#00000000\"" +
                                    mArticalContent +
                                    "</body></html>",
                                    new Html.ImageGetter() {
                                        @Override
                                        public Drawable getDrawable(String source) {
                                            ManagerShareActivity.dbg("img:" + source);
                                            Drawable d = null;
                                            /*
                                            InputStream is = null;
                                            try {
                                                is = (InputStream) new URL(source).getContent();
                                                d = Drawable.createFromStream(is, "src");
                                                d.setBounds(0, 0, d.getIntrinsicWidth(), d.getIntrinsicHeight());
                                                is.close();
                                            } catch (Exception e) {
                                                ManagerShareActivity.error("image: " + source);
                                                Log.e(ManagerShareActivity.TAG, "web", e);
                                                return null;
                                            }
                                            */
                                            return d;
                                        }
                                    }, null
                            ));
                    break;
                case MSG_GET_WEB_CONTENT_FAILED:
                    mArticalContentTextView.setText(R.string.invalid_url);
                    break;
                default:
                    break;
            }
            super.handleMessage(msg);
        }
    };

    private Runnable mGetArticalRunnable = new Runnable() {
        @Override
        public void run() {
            try {
                Document doc = getWebDocument(mPath);
                mArticalTitle = doc.select("h1").first().text();
                mArticalContent = doc.select(".article > p").outerHtml();
                mHandler.sendEmptyMessage(MSG_GET_WEB_CONTENT_DONE);
            } catch (IOException e) {
                ManagerShareActivity.error("Can't connect to " + mPath);
                mHandler.sendEmptyMessage(MSG_GET_WEB_CONTENT_FAILED);
            }
        }
    };
}
