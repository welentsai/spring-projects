import './App.css'

import desserts from './data.json'
import { Dessert, DessertCard } from './components/DessertCard'
import { ShoppingCard } from './components/ShoppingCard'
import { OrderProvider } from './context/OrderContext'

function App() {

  return (
    <OrderProvider>
      <div className='flex flex-col sm:flex-row gap-2'>
        <div className='sm:w-3/4'>
          <h1 className="text-3xl font-bold text-left pb-5">
            Desserts
          </h1>
          <div className="grid grid-cols-1 sm:grid-cols-2 md:grid-cols-3 gap-6">
            {desserts.map((dessert: Dessert, index: number) => (
              <DessertCard
                dessert={dessert} key={index} />
            ))}
          </div>
        </div>
        <div className='sm:w-1/4'>
          <ShoppingCard></ShoppingCard>
        </div>
      </div>
    </OrderProvider>
  )
}

export default App
