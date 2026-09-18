/**
 * 骨架阶段模拟商品数据（后续由后端 /market/product 接口替换）
 * 图片遵循平台规范：使用 text_to_image 生成图片地址。
 */

export interface Product {
  id: number
  name: string
  price: number
  originalPrice: number
  image: string
  shopName: string
  category: string
  desc: string
  stock: number
  sales: number
}

const IMG = 'https://trae-api-cn.mchost.guru/api/ide/v1/text_to_image'

function img(prompt: string): string {
  return `${IMG}?prompt=${encodeURIComponent(prompt)}&image_size=square_hd`
}

export const categories = ['数码', '教材', '服饰', '生活', '运动', '其他']

export const products: Product[] = [
  {
    id: 1,
    name: '九成新山地自行车',
    price: 450,
    originalPrice: 899,
    image: img('a silver mountain bike standing in a campus bicycle rack, product photography, soft daylight, clean background'),
    shopName: '校园骑行社',
    category: '运动',
    desc: '大四毕业转让，骑行里程少，车况良好，配车锁和铃铛，支持校内自提。',
    stock: 1,
    sales: 3
  },
  {
    id: 2,
    name: '机械键盘 87键 青轴',
    price: 120,
    originalPrice: 269,
    image: img('a white mechanical keyboard with blue switches on a desk, product photography, soft lighting, clean background'),
    shopName: '数码小铺',
    category: '数码',
    desc: '青轴机械键盘，87键，灯光正常，键帽全新，出售原因：换了新键盘。',
    stock: 1,
    sales: 12
  },
  {
    id: 3,
    name: '高等数学教材（微积分）',
    price: 15,
    originalPrice: 45,
    image: img('a calculus textbook on a wooden desk, product photography, warm lighting, clean background'),
    shopName: '学长书摊',
    category: '教材',
    desc: '高等数学同济版教材，内有少量笔记，适合复习使用。',
    stock: 5,
    sales: 40
  },
  {
    id: 4,
    name: '宿舍神器 LED 台灯',
    price: 35,
    originalPrice: 79,
    image: img('a modern LED desk lamp glowing on a study desk, product photography, clean background'),
    shopName: '生活优选',
    category: '生活',
    desc: '三档调光，USB 充电，续航持久，宿舍学习必备。',
    stock: 8,
    sales: 25
  },
  {
    id: 5,
    name: '冬季加厚连帽卫衣',
    price: 60,
    originalPrice: 139,
    image: img('a cozy thick hoodie on a hanger, product photography, soft background'),
    shopName: '校园衣橱',
    category: '服饰',
    desc: 'M 码，加绒加厚，只穿过两次，颜色百搭。',
    stock: 2,
    sales: 8
  },
  {
    id: 6,
    name: '降噪头戴式耳机',
    price: 220,
    originalPrice: 499,
    image: img('wireless over-ear noise cancelling headphones, product photography, clean studio background'),
    shopName: '数码小铺',
    category: '数码',
    desc: '主动降噪，续航 30 小时，配件齐全，9 成新。',
    stock: 1,
    sales: 15
  }
]

export function getProductById(id: number): Product | undefined {
  return products.find((p) => p.id === id)
}
